# PanDerm CAV Model V1 

<br />

### 支持的疾病分类 (7类)

| 索引 | 英文名                       | 中文名    |
| -- | ------------------------- | ------ |
| 0  | melanoma                  | 黑色素瘤   |
| 1  | melanocytic\_nevus        | 黑色素痣   |
| 2  | basal\_cell\_carcinoma    | 基底细胞癌  |
| 3  | squamous\_cell\_carcinoma | 鳞状细胞癌  |
| 4  | bowen\_disease            | 鲍文病    |
| 5  | seborrheic\_keratosis     | 脂溢性角化病 |
| 6  | lichen\_planus            | 扁平苔藓   |

***

## 2. 模型架构

```
输入图像 (224×224 RGB)
       │
       ▼
┌─────────────────┐
│  ViT Backbone   │  ← checkpoint-stage2-46.pth
│ (panderm_base)  │     提取 patch tokens
└────────┬────────┘
         │ [1, 196, 768]
         ▼
┌─────────────────┐
│ Patch Projection│  Linear(768→512) + LayerNorm + GELU
└────────┬────────┘
         │ [1, 196, 512]
         ▼
┌─────────────────┐
│ CAV Similarity  │  计算每个patch与77个CAV的余弦相似度
│   (196×77)      │  similarity = cos(patch, cav) + bias
└────────┬────────┘
         │ [1, 196, 77]
         ▼
┌─────────────────┐
│  Top-K Pooling  │  每个概念取响应最高的K=16个patch
│   (K=16)        │  concept_scores = max(top_k_values)
└────────┬────────┘
         │ [1, 77]
         ▼
┌─────────────────┐
│ Top-16 Select    │  只保留激活最高的16个概念
└────────┬────────┘
         │ [1, 16]
         ▼
┌─────────────────┐
│ Disease Voting   │  concept_scores × W_vote.T → 疾病logits
│   (W_vote)      │  7个疾病的加权投票
└────────┬────────┘
         │ [1, 7]
         ▼
    疾病预测结果
```

***

## 3. 核心组件

### 3.1 CAV (Concept Activation Vector)

CAV是77个512维的可学习向量，每个向量代表一个皮肤镜概念特征。

**概念分类**:

| 类别           | 概念数量 | 说明     |
| ------------ | ---- | ------ |
| pigmentation | 约20个 | 色素沉着相关 |
| structural   | 约15个 | 结构模式相关 |
| vascular     | 约12个 | 血管模式相关 |
| color        | 约15个 | 颜色相关   |
| surface      | 约5个  | 表面特征   |
| special      | 约5个  | 特殊特征   |
| other        | 约5个  | 其他     |

**77个概念列表**:

```
C000-C021: 色素网、点球模式、结构模式
C022-C038: 特殊结构、颜色特征
C039-C054: 表面特征、基础颜色
C055-C076: 血管模式、分布特征
```

### 3.2 Top-K Pooling

- 对每个概念，从196个patch中选择响应最高的K=16个patch
- 聚合方式: `concept_score = max(top_k_similarities)`
- 作用: 捕捉每个概念在图像中最显著的局部证据

### 3.3 Disease Voting

- W\_vote: 7×77 的权重矩阵
- 每行代表一个疾病，每列代表一个概念
- 权重由专家知识初始化，可学习微调

***

## 4. 输入输出格式

### 4.1 输入

```python
# 图像预处理
transform = transforms.Compose([
    transforms.Resize((224, 224)),
    transforms.ToTensor(),
    transforms.Normalize(
        mean=[0.48145466, 0.4578275, 0.40821073],  # CLIP normalization
        std=[0.26862954, 0.26130258, 0.27577711]
    )
])

# 输入张量
image_tensor: torch.Size([1, 3, 224, 224])
```

### 4.2 输出

```python
outputs = model(image_tensor)

# 输出字段:
{
    'disease_logits': torch.Size([1, 7]),      # 疾病logits
    'disease_logits_full': torch.Size([1, 7]), # 未mask的logits
    'concept_scores': torch.Size([1, 77]),      # 77概念激活分数
    'top_k_indices': torch.Size([1, 16]),       # Top-16概念索引
    'top_k_scores': torch.Size([1, 16]),         # Top-16概念分数
    'patch_similarity': torch.Size([1, 196, 77]), # patch-概念相似度
    'attention_mask': torch.Size([1, 196, 77]),   # Top-K attention mask
    'patch_features': torch.Size([1, 196, 512])    # patch特征
}
```

### 4.3 疾病概率计算

```python
probs = torch.softmax(outputs['disease_logits'], dim=1)
# probs: [1, 7] - 每个疾病的概率
```

### 4.4 概念名称映射

从 `data/concept_list.json` 获取概念名称:

```python
import json
with open('data/concept_list.json', 'r') as f:
    concepts = json.load(f)
# concepts[0]['name_en'] = "pigment network"
# concepts[0]['name_cn'] = "色素网"
```

***

## 5. 文件结构

```
V1_1_package/
├── cav_v1/
│   ├── __init__.py
│   ├── cav_model.py      # CAVModelV1 模型类定义
│   └── cav_loss.py       # CAVLossV1 损失函数
├── models/
│   ├── __init__.py
│   ├── builder.py        # 模型构建器 get_encoder()
│   └── modeling_finetune.py  # ViT backbone 定义
├── utils/
│   └── utils.py           # 工具函数
├── data/
│   ├── concept_list.json          # 77个概念定义 (含中英文名称)
│   ├── concept_embeddings_77.pt    # CLIP概念嵌入初始化
│   ├── disease_concept_mapping.json      # 疾病-概念映射
│   └── disease_concept_mapping_final_v2.json  # 优化后的映射
├── weights/
│   ├── best_model.pt             # V1_1最佳模型权重
│   └── checkpoint-stage2-46.pth  # 预训练ViT编码器
└── predict_example.py            # 推理示例代码
```

***

## 6. 快速使用

### 6.1 加载模型

```python
import torch
from cav_v1.cav_model import create_cav_model

model = create_cav_model(
    panderm_checkpoint='weights/checkpoint-stage2-46.pth',
    num_concepts=77,
    embed_dim=512,
    freeze_vit=True,
    unfreeze_layers=4,
    top_k=16,
    disease_mapping_path='data/disease_concept_mapping_final_v2.json'
)

checkpoint = torch.load('weights/best_model.pt', map_location='cpu')
model.load_state_dict(checkpoint['model_state_dict'])
model.eval()
```

### 6.2 推理预测

```python
from PIL import Image
from torchvision import transforms

transform = transforms.Compose([
    transforms.Resize((224, 224)),
    transforms.ToTensor(),
    transforms.Normalize(
        mean=[0.48145466, 0.4578275, 0.40821073],
        std=[0.26862954, 0.26130258, 0.27577711]
    )
])

# 加载并预处理图像
img = Image.open('test.jpg').convert('RGB')
img_tensor = transform(img).unsqueeze(0)

# 推理
with torch.no_grad():
    outputs = model(img_tensor)

# 获取疾病预测
probs = torch.softmax(outputs['disease_logits'], dim=1)
pred_idx = probs.argmax(dim=1).item()
confidence = probs[0, pred_idx].item()

DISEASE_NAMES = {
    0: 'melanoma',
    1: 'melanocytic_nevus',
    2: 'basal_cell_carcinoma',
    3: 'squamous_cell_carcinoma',
    4: 'bowen_disease',
    5: 'seborrheic_keratosis',
    6: 'lichen_planus'
}

print(f"预测: {DISEASE_NAMES[pred_idx]}, 置信度: {confidence:.2%}")
```

### 6.3 获取概念特征

```python
import json

with open('data/concept_list.json', 'r') as f:
    concepts = json.load(f)

top_k_scores = outputs['top_k_scores'].squeeze()
top_k_indices = outputs['top_k_indices'].squeeze()

print("Top-16 激活概念:")
for i in range(16):
    idx = top_k_indices[i].item()
    score = top_k_scores[i].item()
    print(f"  {concepts[idx]['name_en']}: {score:.4f}")
```

***

## 7. 模型特性

### 7.1 可解释性

- 每个疾病预测都附带激活的概念证据
- 可视化 `patch_similarity` 可显示每个概念在图像中的空间分布

### 7.2 概念激活机制

```
图像 → 196个patch tokens → 每个token与77个CAV计算相似度
                              ↓
                     196×77 相似度矩阵
                              ↓
                     Top-K Pooling
                              ↓
                     77个概念分数
                              ↓
                     Top-16 选择
                              ↓
                     疾病投票
```

### 7.3 CAV初始化

- 初始使用CLIP text embeddings初始化77个CAV向量
- 训练过程中可学习微调

***

## 8. 依赖环境

```
torch >= 1.9
torchvision
timm
open_clip
PIL (Pillow)
numpy
```

***

## 9. 注意事项

1. **输入尺寸**: 必须为224×224 RGB图像
2. **归一化**: 必须使用CLIP归一化参数
3. **CUDA支持**: GPU推理速度更快，但CPU也可运行
4. **概念列表**: V1\_1版本使用前77个概念(C000-C076)

***


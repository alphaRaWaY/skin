"""
ViLC-BM (Visual-Language Concept Bottleneck Model)

核心设计:
1. 每个patch token与所有概念向量计算余弦相似度 → 概念响应矩阵
2. Top-K池化: 对每个概念，取响应最高的K个patch聚合 → 图像级概念激活分数
3. 疾病投票: 概念得分与w_vote权重相乘 → 疾病预测

这实现了:
- 每个patch与所有概念的全局匹配
- Top-K选择每个概念对应响应高的K个patch
- 模型能判断"某个概念是否在图像的某个局部区域被证据支持"
"""

import os
import sys
import torch
import torch.nn as nn
import torch.nn.functional as F
from typing import Dict, Tuple, Optional
from pathlib import Path

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


class TopKPooling(nn.Module):
    """
    Top-K Pooling

    对每个概念c，取响应最高的K个patch进行聚合
    作用: 实现patch级别的选择性注意力
    """

    def __init__(self, k: int = 16):
        super().__init__()
        self.k = k

    def forward(self, similarity_matrix: torch.Tensor) -> Tuple[torch.Tensor, torch.Tensor]:
        """
        输入: similarity_matrix [B, num_patches, num_concepts]
        输出:
            concept_scores [B, num_concepts] - 每个概念的激活分数
            attention_mask [B, num_patches, num_concepts] - one-hot掩码
        """
        B, num_patches, num_concepts = similarity_matrix.shape

        k = max(1, min(self.k, num_patches))

        _, sorted_indices = torch.sort(similarity_matrix, dim=1, descending=True)
        top_indices = sorted_indices[:, :k, :].contiguous()
        top_values = torch.gather(similarity_matrix, 1, top_indices)

        if k == 1:
            concept_scores = top_values.squeeze(1)
        else:
            concept_scores = top_values.max(dim=1).values

        mask = torch.zeros_like(similarity_matrix)
        mask.scatter_(1, top_indices, 1.0)

        return concept_scores, mask


class ViLCBM(nn.Module):
    """
    ViLC-BM (Visual-Language Concept Bottleneck Model) - 视觉-语言概念瓶颈模型

    架构流程:
    1. Image → ViT → patch_tokens [B, 196, 768]
    2. Patch Projection → [B, 196, 512]
    3. Concept Similarity → [B, 196, num_concepts] (每个patch与每个概念向量的余弦相似度)
    4. Top-K Pooling → [B, num_concepts] (每个概念的激活分数)
    5. Disease Voting → [B, num_diseases] (疾病预测)

    命名含义:
    - Vi: Visual (视觉) - ViT-B/16视觉编码器
    - L: Language (语言) - GPT文本描述→BiomedCLIP语义空间
    - C: Concept (概念) - 可学习医学概念向量
    - BM: Bottleneck Model (瓶颈模型) - 概念作为信息瓶颈桥接视觉与预测
    """

    def __init__(
        self,
        panderm_checkpoint: str,
        num_concepts: int = 77,
        embed_dim: int = 512,
        freeze_vit: bool = True,
        unfreeze_layers: int = 4,
        top_k: int = 16,
        disease_mapping_path: str = None,
        concept_embeddings_path: str = None,
        num_diseases: int = None,
        top_k_concepts: int = None
    ):
        super().__init__()

        self.num_concepts = num_concepts
        self.embed_dim = embed_dim
        self.top_k = top_k
        self.top_k_concepts = top_k_concepts if top_k_concepts is not None else top_k
        self.num_diseases = num_diseases

        if concept_embeddings_path:
            self.custom_concept_emb_path = concept_embeddings_path

        self._build_backbone(panderm_checkpoint, freeze_vit, unfreeze_layers, num_diseases)

        self.patch_projection = nn.Sequential(
            nn.Linear(768, 512),
            nn.LayerNorm(512),
            nn.GELU()
        )

        self._init_concept_vectors()

        self.concept_bias = nn.Parameter(torch.zeros(num_concepts))

        self.top_k_pooling = TopKPooling(k=top_k)

        self._init_disease_voting(disease_mapping_path, num_diseases)

        print(f"[ViLC-BM] 初始化完成")
        print(f"  概念数量: {num_concepts}")
        print(f"  Top-K: {top_k}")
        print(f"  概念向量初始化: CLIP text embeddings")

    def _build_backbone(self, checkpoint_path: str, freeze_vit: bool, unfreeze_layers: int, num_diseases: int = None):
        """构建ViT backbone"""
        from models.modeling_finetune import panderm_base_patch16_224

        if num_diseases is None:
            num_diseases = 7

        self.visual_encoder = panderm_base_patch16_224(num_classes=num_diseases)

        if os.path.exists(checkpoint_path):
            checkpoint = torch.load(checkpoint_path, map_location='cpu', weights_only=False)
            if 'model' in checkpoint:
                state_dict = checkpoint['model']
            elif 'state_dict' in checkpoint:
                state_dict = checkpoint['state_dict']
            else:
                state_dict = checkpoint

            # 过滤掉分类头，因为预训练权重是7分类的，当前模型可能是其他分类数
            filtered_state_dict = {}
            skip_keys = {'head.weight', 'head.bias'}
            for k, v in state_dict.items():
                if k not in skip_keys:
                    filtered_state_dict[k] = v

            if len(filtered_state_dict) == 0:
                print(f"[ViLC-BM] 警告: 过滤后没有可加载的权重，跳过预训练权重加载")
            else:
                new_state_dict = {}
                for k, v in filtered_state_dict.items():
                    if k.startswith('visual_encoder.'):
                        new_k = k.replace('visual_encoder.', '')
                    elif k.startswith('encoder.'):
                        new_k = k.replace('encoder.', '')
                    else:
                        new_k = k
                    new_state_dict[new_k] = v

                result = self.visual_encoder.load_state_dict(new_state_dict, strict=False)
                if len(result.missing_keys) > 0:
                    print(f"[ViLC-BM] 部分权重未加载: {result.missing_keys[:3]}...")
                print(f"[ViLC-BM] 加载预训练权重: {checkpoint_path} (跳过分类头: {skip_keys})")

        if freeze_vit:
            for name, param in self.visual_encoder.named_parameters():
                if 'blocks' in name:
                    layer_idx = int(name.split('.')[1])
                    if layer_idx >= 12 - unfreeze_layers:
                        continue
                param.requires_grad = False
            print(f"[ViLC-BM] ViT编码器部分冻结，解冻最后 {unfreeze_layers} 层")
        else:
            print(f"[ViLC-BM] ViT编码器完全解冻 (unfreeze_layers={unfreeze_layers})")

    def _init_concept_vectors(self):
        """
        初始化概念向量 (原CAV向量)

        概念向量:
        - 每个概念对应一个512维向量
        - 从CLIP/BiomedCLIP text embeddings初始化
        - 在训练过程中可学习更新
        """
        self.concept_vectors = nn.Parameter(torch.randn(self.num_concepts, self.embed_dim) * 0.02)

        self._init_concepts_from_clip()

    def _init_concepts_from_clip(self):
        """从CLIP/BiomedCLIP text embeddings初始化概念向量"""
        default_path = Path(__file__).parent.parent / "data" / "concepts_used" / "concept_embeddings_77.pt"

        emb_path = getattr(self, 'custom_concept_emb_path', None) or default_path

        if isinstance(emb_path, str):
            emb_path = Path(emb_path)

        if emb_path.exists():
            clip_emb = torch.load(emb_path, map_location='cpu', weights_only=False)
            if clip_emb.shape[0] >= self.num_concepts:
                self.concept_vectors.data[:self.num_concepts] = F.normalize(
                    clip_emb[:self.num_concepts], dim=-1
                )
                print(f"[ViLC-BM] 概念向量从CLIP初始化: {emb_path}")
            else:
                self.concept_vectors.data[:clip_emb.shape[0]] = F.normalize(
                    clip_emb, dim=-1
                )
                print(f"[ViLC-BM] 概念向量部分从CLIP初始化")

    def compute_concept_similarity(self, patch_features: torch.Tensor) -> torch.Tensor:
        """
        计算patch-概念余弦相似度

        核心: 每个patch token与所有概念向量计算余弦相似度
        输出: [B, num_patches, num_concepts] 概念响应矩阵

        这个矩阵实现了:
        - 每个patch与所有概念的全局匹配
        - 后续Top-K池化会选择每个概念响应高的K个patch
        """
        patch_norm = F.normalize(patch_features, dim=-1, p=2)

        concept_norm = self.concept_vectors / (self.concept_vectors.norm(dim=-1, keepdim=True) + 1e-8)

        similarity = torch.matmul(patch_norm, concept_norm.T)

        similarity = similarity + self.concept_bias

        return similarity

    def _init_disease_voting(self, mapping_path: str, num_diseases: int = None):
        """初始化疾病-概念投票矩阵"""
        if num_diseases is None:
            num_diseases = 7

        self.num_diseases = num_diseases
        self.w_vote = nn.Parameter(torch.ones(num_diseases, self.num_concepts) * 0.01)

        if mapping_path and os.path.exists(mapping_path):
            self._load_disease_mapping(mapping_path)
        else:
            self._init_uniform_weights()

    def _init_uniform_weights(self):
        """用均匀小权重初始化所有概念"""
        weight_matrix = torch.zeros(self.num_diseases, self.num_concepts)
        for d in range(self.num_diseases):
            weight_matrix[d] = 0.01
        self.w_vote.data = weight_matrix
        print(f"[ViLC-BM] w_vote均匀小权重初始化, 非零权重: {(weight_matrix > 0).sum().item()}")

    def _load_disease_mapping(self, mapping_path: str):
        """从专家知识加载疾病-概念映射 - 直接使用concept_idx"""
        import json

        with open(mapping_path, 'r', encoding='utf-8') as f:
            mapping = json.load(f)

        # 从 config.json 读取疾病列表和概念列表
        disease_classes = mapping.get('diseases', [
            "melanoma", "melanocytic_nevus", "basal_cell_carcinoma",
            "squamous_cell_carcinoma", "bowen_disease",
            "seborrheic_keratosis", "lichen_planus"
        ])

        # 读取concept_list
        concept_list_from_config = mapping.get('concept_list', [])
        self.concept_names_map = {}
        for i, name in enumerate(concept_list_from_config[:self.num_concepts]):
            self.concept_names_map[i] = name

        weight_matrix = torch.zeros(self.num_diseases, self.num_concepts)
        used_concepts = set()

        # 获取 disease_concept_mapping
        disease_mapping = mapping.get('disease_concept_mapping', {})

        for disease_name, disease_data in disease_mapping.items():
            if disease_name not in disease_classes:
                continue
            idx = disease_classes.index(disease_name)

            concepts = disease_data.get('concepts', []) if isinstance(disease_data, dict) else disease_data

            if isinstance(concepts, list):
                for item in concepts:
                    if isinstance(item, dict):
                        # 直接使用 concept_idx，不做字符串匹配！
                        c_idx = item.get('concept_idx')
                        weight = item.get('weight', 1.0)
                        if c_idx is not None and 0 <= c_idx < self.num_concepts:
                            weight_matrix[idx, c_idx] = weight
                            used_concepts.add(c_idx)
                        else:
                            print(f"[Warning] Invalid concept_idx {c_idx} for disease {disease_name}")
                    else:
                        print(f"[Warning] Invalid concept item: {item}")

        unused_concepts = set(range(self.num_concepts)) - used_concepts
        if unused_concepts:
            print(f"[Info] {len(unused_concepts)} concepts not in mapping, setting negative weights")
            for c_idx in unused_concepts:
                for d in range(self.num_diseases):
                    weight_matrix[d, c_idx] = -0.1

        self.w_vote.data = weight_matrix
        print(f"[ViLC-BM] w_vote从专家映射初始化, 非零权重: {(weight_matrix > 0).sum().item()}")

    def forward(self, images: torch.Tensor, return_intermediate: bool = False) -> Dict[str, torch.Tensor]:
        """
        前向传播

        完整流程:
        1. ViT编码 → patch_tokens [B, 196, 768]
        2. Patch投影 → [B, 196, 512]
        3. Concept相似度 → [B, 196, num_concepts]  ← 核心概念响应矩阵
        4. Top-K池化 → [B, num_concepts] ← 图像级概念激活
        5. Top-K概念选择 → [B, 16] ← 只保留激活最高的16个概念
        6. 疾病投票(masked) → [B, num_diseases]
        """
        B = images.size(0)

        if hasattr(self.visual_encoder, 'forward_features'):
            patch_tokens = self.visual_encoder.forward_features(images, return_all_tokens=True)
        else:
            patch_tokens = self.visual_encoder(images)

        if len(patch_tokens.shape) == 4:
            patch_tokens = patch_tokens[:, 1:].transpose(1, 2)
            B, C, H, W = patch_tokens.shape
            patch_tokens = patch_tokens.reshape(B, H * W, C)
        elif len(patch_tokens.shape) == 3:
            if patch_tokens.shape[2] == 768:
                patch_tokens = patch_tokens
            elif patch_tokens.shape[1] == 768:
                patch_tokens = patch_tokens.transpose(1, 2)
            else:
                patch_tokens = patch_tokens
        else:
            patch_tokens = patch_tokens.reshape(B, 196, 768)

        patch_features = self.patch_projection(patch_tokens)
        patch_features = torch.nan_to_num(patch_features, nan=0.0, posinf=1.0, neginf=-1.0)

        patch_similarity = self.compute_concept_similarity(patch_features)
        patch_similarity = torch.nan_to_num(patch_similarity, nan=0.0, posinf=1.0, neginf=-1.0)

        concept_scores, attention_mask = self.top_k_pooling(patch_similarity)

        concept_scores = torch.nan_to_num(concept_scores, nan=0.0, posinf=1.0, neginf=-1.0)

        k_safe = max(1, min(self.top_k_concepts, concept_scores.shape[1]))
        _, sorted_indices = torch.sort(concept_scores, dim=1, descending=True)
        top_k_indices = sorted_indices[:, :k_safe]
        top_k_scores = torch.gather(concept_scores, 1, top_k_indices)

        concept_scores_masked = torch.zeros_like(concept_scores)
        concept_scores_masked.scatter_(1, top_k_indices, concept_scores.gather(1, top_k_indices))

        w_vote_clean = torch.nan_to_num(self.w_vote, nan=0.0, posinf=1.0, neginf=-1.0)
        disease_logits = torch.matmul(concept_scores_masked, w_vote_clean.T)
        disease_logits = torch.clamp(disease_logits, min=-50, max=50)

        disease_positive_concepts = self.w_vote > 0.1

        B = disease_logits.shape[0]
        num_diseases = disease_logits.shape[1]
        valid_disease_mask = torch.zeros(B, num_diseases, dtype=torch.bool, device=disease_logits.device)
        for d in range(num_diseases):
            disease_concepts = disease_positive_concepts[d]
            for b in range(B):
                tk = top_k_indices[b]
                if disease_concepts[tk].any():
                    valid_disease_mask[b, d] = True

        row_all_invalid = ~valid_disease_mask.any(dim=1)
        valid_disease_mask[row_all_invalid] = True

        masked_logits = disease_logits

        outputs = {
            'disease_logits': masked_logits,
            'disease_logits_full': disease_logits,
            'concept_scores': torch.nan_to_num(concept_scores, nan=0.0, posinf=1.0, neginf=-1.0),
            'top_k_indices': top_k_indices,
            'top_k_scores': torch.nan_to_num(top_k_scores, nan=0.0, posinf=1.0, neginf=-1.0),
            'patch_similarity': patch_similarity,
            'attention_mask': attention_mask,
            'patch_features': patch_features
        }

        if return_intermediate:
            outputs['concept_vectors'] = self.concept_vectors

        return outputs


# 保持向后兼容的别名
ViLCBM = ViLCBM


def create_vilc_bm(
    panderm_checkpoint: str,
    num_concepts: int = 77,
    embed_dim: int = 512,
    freeze_vit: bool = True,
    unfreeze_layers: int = 4,
    top_k: int = 16,
    disease_mapping_path: str = None,
    num_diseases: int = None,
    concept_embeddings_path: str = None
) -> ViLCBM:
    """ViLC-BM模型工厂函数"""
    return ViLCBM(
        panderm_checkpoint=panderm_checkpoint,
        num_concepts=num_concepts,
        embed_dim=embed_dim,
        freeze_vit=freeze_vit,
        unfreeze_layers=unfreeze_layers,
        top_k=top_k,
        disease_mapping_path=disease_mapping_path,
        num_diseases=num_diseases,
        concept_embeddings_path=concept_embeddings_path,
    )


# 向后兼容的工厂函数
create_vilc_bm = create_vilc_bm

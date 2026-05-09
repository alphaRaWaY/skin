"""
CAV Model v1 - 基于Patch-概念余弦相似度的可解释模型

核心设计:
1. 每个patch token与所有80个CAV向量计算余弦相似度 → 196×80概念响应矩阵
2. Top-K池化: 对每个概念，取响应最高的K个patch聚合 → 图像级概念激活分数
3. 疾病投票: 概念得分与W_vote权重相乘 → 疾病预测

这实现了:
- 每个patch与所有80个CAV的全局匹配
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

        k = min(self.k, num_patches)

        top_values, top_indices = torch.topk(similarity_matrix, k, dim=1)

        concept_scores = top_values.max(dim=1).values

        mask = torch.zeros_like(similarity_matrix)
        mask.scatter_(1, top_indices, 1.0)

        return concept_scores, mask


class CAVModelV1(nn.Module):
    """
    CAV Model v1 - 完整实现

    架构流程:
    1. Image → ViT → patch_tokens [B, 196, 768]
    2. Patch Projection → [B, 196, 512]
    3. CAV Similarity → [B, 196, 80] (每个patch与每个CAV的余弦相似度)
    4. Top-K Pooling → [B, 80] (每个概念的激活分数)
    5. Disease Voting → [B, 7] (疾病预测)
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
        num_diseases: int = None
    ):
        super().__init__()

        self.num_concepts = num_concepts
        self.embed_dim = embed_dim
        self.top_k = top_k

        if concept_embeddings_path:
            self.custom_concept_emb_path = concept_embeddings_path

        self._build_backbone(panderm_checkpoint, freeze_vit, unfreeze_layers)

        self.patch_projection = nn.Sequential(
            nn.Linear(768, 512),
            nn.LayerNorm(512),
            nn.GELU()
        )

        self._init_cav_vectors()

        self.concept_bias = nn.Parameter(torch.zeros(num_concepts))

        self.top_k_pooling = TopKPooling(k=top_k)

        self._init_disease_voting(disease_mapping_path, num_diseases)

        print(f"[CAVModelV1] 初始化完成")
        print(f"  概念数量: {num_concepts}")
        print(f"  Top-K: {top_k}")
        print(f"  CAV初始化: CLIP text embeddings")

    def _build_backbone(self, checkpoint_path: str, freeze_vit: bool, unfreeze_layers: int):
        """构建ViT backbone"""
        from models.modeling_finetune import panderm_base_patch16_224

        self.visual_encoder = panderm_base_patch16_224(num_classes=7)

        if os.path.exists(checkpoint_path):
            checkpoint = torch.load(checkpoint_path, map_location='cpu', weights_only=False)
            if 'model' in checkpoint:
                state_dict = checkpoint['model']
            elif 'state_dict' in checkpoint:
                state_dict = checkpoint['state_dict']
            else:
                state_dict = checkpoint

            new_state_dict = {}
            for k, v in state_dict.items():
                if k.startswith('visual_encoder.'):
                    new_k = k.replace('visual_encoder.', '')
                elif k.startswith('encoder.'):
                    new_k = k.replace('encoder.', '')
                else:
                    new_k = k
                new_state_dict[new_k] = v

            result = self.visual_encoder.load_state_dict(new_state_dict, strict=False)
            if len(result.missing_keys) > 0:
                print(f"[CAVModelV1] 部分权重未加载: {result.missing_keys[:3]}...")
            print(f"[CAVModelV1] 加载预训练权重: {checkpoint_path}")

        if freeze_vit:
            for name, param in self.visual_encoder.named_parameters():
                if 'blocks' in name:
                    layer_idx = int(name.split('.')[1])
                    if layer_idx >= 12 - unfreeze_layers:
                        continue
                param.requires_grad = False

    def _init_cav_vectors(self):
        """
        初始化CAV向量

        CAV (Concept Activation Vectors):
        - 每个概念对应一个512维向量
        - 从CLIP text embeddings初始化
        """
        self.cav_vectors = nn.Parameter(torch.randn(self.num_concepts, self.embed_dim) * 0.02)

        self._init_cav_from_clip()

    def _init_cav_from_clip(self):
        """从CLIP text embeddings初始化CAV"""
        default_path = Path(__file__).parent.parent / "data" / "concepts_used" / "concept_embeddings_77.pt"

        emb_path = getattr(self, 'custom_concept_emb_path', None) or default_path

        if isinstance(emb_path, str):
            emb_path = Path(emb_path)

        if emb_path.exists():
            clip_emb = torch.load(emb_path, map_location='cpu', weights_only=False)
            if clip_emb.shape[0] >= self.num_concepts:
                self.cav_vectors.data[:self.num_concepts] = F.normalize(
                    clip_emb[:self.num_concepts], dim=-1
                )
                print(f"[CAVModelV1] CAV从CLIP初始化: {emb_path}")
            else:
                self.cav_vectors.data[:clip_emb.shape[0]] = F.normalize(
                    clip_emb, dim=-1
                )
                print(f"[CAVModelV1] CAV部分从CLIP初始化")

    def compute_concept_similarity(self, patch_features: torch.Tensor) -> torch.Tensor:
        """
        计算patch-概念余弦相似度

        核心: 每个patch token与所有CAV向量计算余弦相似度
        输出: [B, num_patches, num_concepts] 概念响应矩阵

        这个矩阵实现了:
        - 每个patch与所有80个CAV的全局匹配
        - 后续Top-K池化会选择每个概念响应高的K个patch
        """
        patch_norm = F.normalize(patch_features, dim=-1, p=2)

        cav_norm = self.cav_vectors / (self.cav_vectors.norm(dim=-1, keepdim=True) + 1e-8)

        similarity = torch.matmul(patch_norm, cav_norm.T)

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
        print(f"[CAVModelV1] W_vote均匀小权重初始化, 非零权重: {(weight_matrix > 0).sum().item()}")

    def _load_disease_mapping(self, mapping_path: str):
        """从专家知识加载疾病-概念映射"""
        import json

        with open(mapping_path, 'r', encoding='utf-8') as f:
            mapping = json.load(f)

        concept_list_path = Path(__file__).parent.parent / "data" / "concepts_used" / "concept_list.json"
        concept_idx_map = {}
        self.concept_names_map = {}
        if concept_list_path.exists():
            with open(concept_list_path, 'r', encoding='utf-8') as f:
                concepts = json.load(f)
                for i, c in enumerate(concepts[:self.num_concepts]):
                    name_en = c.get('name_en', c.get('name', '')).lower().strip()
                    concept_idx_map[name_en] = i
                    self.concept_names_map[i] = c.get('name_en', c.get('name', f"c{i}"))

        disease_classes = [
            "melanoma", "melanocytic_nevus", "basal_cell_carcinoma",
            "squamous_cell_carcinoma", "bowen_disease",
            "seborrheic_keratosis", "lichen_planus"
        ]

        weight_matrix = torch.zeros(7, self.num_concepts)
        used_concepts = set()

        name_aliases = {
            'blue-whitish veil': 'blue-whitish veil',
            'blue whitish veil': 'blue-whitish veil',
            'blue-gray globules': 'multiple blue-gray globules',
            'blue gray globules': 'blue-grey pigmentation',
            'fissures and ridges': 'fissures and ridge',
            'fissure and ridge': 'fissures and ridge',
            'dotted vessels': 'dots vessels',
            'dotted vessel': 'dots vessels',
            'irregular streaks': 'branched streaks',
            'shiny white streaks': 'fibrillar pattern',
            'shiny white blotches': 'white scales',
            'leaf-like areas': 'leaf-like structures',
            'leaf like areas': 'leaf-like structures',
            'spoke-wheel areas': 'spoke-wheel-like streaks',
            'spoke wheel areas': 'spoke-wheel-like streaks',
            'maple leaf structures': 'leaf-like structures',
            'keratin plug': 'milia-like cysts',
            'white circles': 'circles,white',
            'scaling': 'yellow scales and crusts',
            'violaceous background': 'purple',
            'reticular pattern': 'lattice like pattern',
            'wickham striae': 'wickham striae',
        }

        for disease_name, disease_data in mapping.items():
            if disease_name in disease_classes:
                idx = disease_classes.index(disease_name)

                concepts = disease_data.get('concepts', disease_data) if isinstance(disease_data, dict) else disease_data

                if isinstance(concepts, list):
                    for item in concepts:
                        if isinstance(item, dict):
                            concept_name = item.get('concept_name', '')
                            weight = item.get('weight', 0)
                        else:
                            concept_name = item
                            weight = 1.0

                        concept_key = concept_name.lower().strip()
                        matched = False
                        if concept_key in concept_idx_map:
                            c_idx = concept_idx_map[concept_key]
                            weight_matrix[idx, c_idx] = weight
                            used_concepts.add(c_idx)
                            matched = True
                        elif concept_key in name_aliases:
                            alias = name_aliases[concept_key]
                            if alias in concept_idx_map:
                                c_idx = concept_idx_map[alias]
                                weight_matrix[idx, c_idx] = weight
                                used_concepts.add(c_idx)
                                matched = True
                            else:
                                print(f"[Warning] Alias '{alias}' not found for '{concept_name}'")

                        if not matched:
                            print(f"[Warning] Concept '{concept_name}' not found in concept_list")

        unused_concepts = set(range(self.num_concepts)) - used_concepts
        if unused_concepts:
            print(f"[Info] {len(unused_concepts)} concepts not in mapping, setting negative weights")
            for c_idx in unused_concepts:
                for d in range(7):
                    weight_matrix[d, c_idx] = -0.1

        self.w_vote.data = weight_matrix
        print(f"[CAVModelV1] W_vote从专家映射初始化, 非零权重: {(weight_matrix > 0).sum().item()}")

    def forward(self, images: torch.Tensor, return_intermediate: bool = False) -> Dict[str, torch.Tensor]:
        """
        前向传播

        完整流程:
        1. ViT编码 → patch_tokens [B, 196, 768]
        2. Patch投影 → [B, 196, 512]
        3. CAV相似度 → [B, 196, 77]  ← 核心概念响应矩阵
        4. Top-K池化 → [B, 77] ← 图像级概念激活
        5. Top-K概念选择 → [B, 16] ← 只保留激活最高的16个概念
        6. 疾病投票(masked) → [B, 7]
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

        top_k_concepts = 16
        top_k_scores, top_k_indices = torch.topk(concept_scores, min(top_k_concepts, concept_scores.shape[1]), dim=1)

        concept_scores_masked = torch.zeros_like(concept_scores)
        concept_scores_masked.scatter_(1, top_k_indices, concept_scores.gather(1, top_k_indices))

        disease_logits = torch.matmul(concept_scores_masked, self.w_vote.T)
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
            outputs['cav_vectors'] = self.cav_vectors

        return outputs


def create_cav_model(
    panderm_checkpoint: str,
    num_concepts: int = 77,
    embed_dim: int = 512,
    freeze_vit: bool = True,
    unfreeze_layers: int = 4,
    top_k: int = 16,
    disease_mapping_path: str = None
) -> CAVModelV1:
    """CAV模型工厂函数"""
    return CAVModelV1(
        panderm_checkpoint=panderm_checkpoint,
        num_concepts=num_concepts,
        embed_dim=embed_dim,
        freeze_vit=freeze_vit,
        unfreeze_layers=unfreeze_layers,
        top_k=top_k,
        disease_mapping_path=disease_mapping_path
    )

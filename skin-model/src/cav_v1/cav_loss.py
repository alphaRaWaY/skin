"""
CAV Loss v1 - 损失函数实现

损失设计:
1. L_cls: Focal Loss (分类损失，高响应错误分类抑制)
2. L_align: CAV对齐正确的patch (文本端初始化向视觉端对齐)
3. L_text: InfoNCE loss (保持CAV与CLIP text embedding的对齐)

对齐损失机制:
- 分类错误 → 正确疾病存在血管特征(含权重影响)
- patch与CAV_血管相似度低 → score[血管]低
- 损失大 → 梯度推动CAV_血管向高响应patch迁移
- 结果: 实现视觉-语言的对齐
"""

import os
import sys
import torch
import torch.nn as nn
import torch.nn.functional as F
from typing import Dict, Optional

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


class CAVLossV1(nn.Module):
    """
    CAV v1 损失函数

    L_total = L_cls + α·L_align + β·L_text

    设计原则:
    - L_cls: 分类准确
    - L_align: CAV学习正确的patch对应关系
    - L_text: CAV保持与CLIP语义空间的对齐
    """

    CLASS_WEIGHTS = {
        0: 2.5,   # melanoma (93%) - 提高权重
        1: 2.0,   # melanocytic_nevus (94%) - 提高权重
        2: 1.0,   # basal_cell_carcinoma (96%)
        3: 5.0,   # squamous_cell_carcinoma (87%)
        4: 4.0,   # bowen_disease (94%)
        5: 5.0,   # seborrheic_keratosis (82%)
        6: 5.0    # lichen_planus (83%)
    }

    CONFUSION_PAIR_WEIGHTS = {
        (0, 1): 3.0,   # melanoma <-> melanocytic_nevus 最高权重
        (1, 0): 3.0,
        (0, 2): 2.0,   # melanoma <-> basal_cell_carcinoma
        (2, 0): 2.0,
        (1, 2): 2.0,   # melanocytic_nevus <-> basal_cell_carcinoma
        (2, 1): 2.0,
        (5, 0): 2.0,   # seborrheic_keratosis <-> melanoma
        (0, 5): 2.0,
        (4, 6): 2.0,   # bowen_disease <-> lichen_planus
        (6, 4): 2.0,
    }

    def __init__(
        self,
        alpha: float = 0.3,
        beta: float = 1.0,
        info_nce_tau: float = 0.07,
        use_text_anchor: bool = True
    ):
        super().__init__()

        self.alpha = alpha
        self.beta = beta
        self.tau = info_nce_tau
        self.use_text_anchor = use_text_anchor

        weights = torch.tensor([self.CLASS_WEIGHTS[i] for i in range(7)])
        self.register_buffer('class_weights', weights)

        self.text_embeddings = None

        print(f"[CAVLossV1] 损失函数初始化")
        print(f"  α (对齐损失): {alpha}")
        print(f"  β (文本锚损失): {beta}")
        print(f"  τ (InfoNCE温度): {info_nce_tau}")

    def set_text_embeddings(self, text_emb: torch.Tensor):
        """设置CLIP文本嵌入用于InfoNCE损失"""
        self.text_embeddings = text_emb

    def focal_loss(self, logits: torch.Tensor, labels: torch.Tensor) -> torch.Tensor:
        """
        Focal Loss with Hard Sample Mining - 分类损失

        特点:
        - 抑制易分类样本的损失
        - 高响应错误分类样本损失更大
        - melanoma <-> nevus 混淆对额外加权
        """
        ce_loss = F.cross_entropy(logits, labels, reduction='none')

        pt = torch.exp(-ce_loss)

        focal_weight = (1 - pt) ** 2.5

        class_weights = self.class_weights.to(logits.device)
        sample_weights = class_weights[labels]

        confusion_weights = torch.ones_like(labels, dtype=torch.float32)
        for b in range(len(labels)):
            true_label = labels[b].item()
            pred_label = logits[b].argmax().item()
            pair = (true_label, pred_label)
            if pair in self.CONFUSION_PAIR_WEIGHTS:
                confusion_weights[b] = self.CONFUSION_PAIR_WEIGHTS[pair]

        confidence_penalty = torch.ones_like(labels, dtype=torch.float32)
        probs = F.softmax(logits, dim=1)
        max_probs = probs.max(dim=1).values
        boundary_mask = (max_probs < 0.7).float()
        confidence_penalty = confidence_penalty + boundary_mask * 0.5

        focal_loss = 0.25 * focal_weight * ce_loss * sample_weights * confusion_weights * confidence_penalty

        return focal_loss.mean()

    def cross_view_alignment_loss(
        self,
        patch_features: torch.Tensor,
        cav_vectors: torch.Tensor,
        patch_similarity: torch.Tensor,
        labels: torch.Tensor,
        w_vote: torch.Tensor,
        top_k: int = 16,
        top_k_concepts: int = 16,
        concept_scores: torch.Tensor = None
    ) -> torch.Tensor:
        """
        Cross-View Alignment Loss - CAV对齐损失

        机制:
        1. 只考虑激活最高的top_k_concepts个概念
        2. 正样本（该疾病映射的概念）：拉近CAV与高响应patch的距离
        3. 负样本（其他概念）：统一推远CAV与patch的距离
        4. 损失 = Σ(正权重 × (1-sim)) / pos_count + λ × Σ(1+sim) / neg_count
        """
        B, num_patches, num_concepts = patch_similarity.shape
        device = patch_similarity.device

        patch_norm = F.normalize(patch_features, dim=-1)

        cav_norm = cav_vectors / (cav_vectors.norm(dim=-1, keepdim=True) + 1e-8)
        w_vote_norm = F.normalize(w_vote, dim=-1)

        top_values, top_indices = torch.topk(patch_similarity, top_k, dim=1)

        _, top_c_indices = torch.topk(concept_scores, min(top_k_concepts, concept_scores.shape[1]), dim=1)

        disease_classes = 7
        positive_concepts = {}
        for d in range(disease_classes):
            pos_mask = w_vote_norm[d] > 0.1
            positive_concepts[d] = pos_mask.nonzero().squeeze(-1).tolist()

        loss_align = 0.0
        pos_count = 0
        neg_count = 0

        for b in range(B):
            label = labels[b].item()
            pos_indices = positive_concepts[label]
            top_c = top_c_indices[b].tolist()

            for c in top_c:
                if c in pos_indices:
                    pos_count += 1
                else:
                    neg_count += 1

        pos_count = max(pos_count, 1)
        neg_count = max(neg_count, 1)
        neg_weight = 0.1

        for b in range(B):
            label = labels[b].item()
            disease_weights = w_vote_norm[label]
            pos_indices = positive_concepts[label]
            top_c = top_c_indices[b].tolist()

            for c in top_c:
                patch_idx = top_indices[b, :top_k, c]
                selected_patches = patch_norm[b, patch_idx]
                mean_patch = selected_patches.mean(dim=0)

                cav_c = cav_norm[c]
                sim = F.cosine_similarity(
                    mean_patch.unsqueeze(0),
                    cav_c.unsqueeze(0),
                    dim=-1
                )

                if c in pos_indices:
                    loss_align += disease_weights[c] * (1 - sim) / pos_count
                else:
                    loss_align += neg_weight * (1 + sim) / neg_count

        return loss_align

    def info_nce_loss(self, cav_vectors: torch.Tensor, top_k_indices: torch.Tensor = None) -> torch.Tensor:
        """
        InfoNCE Loss - 文本锚损失

        目的:
        - 只对Top-K激活概念保持与原始CLIP text embedding的对齐
        - 拉开不同概念之间的距离
        - 防止CAV语义漂移

        正例: 自己对应的CLIP文本嵌入
        负例: 其他概念的CLIP文本嵌入
        """
        if self.text_embeddings is None:
            return torch.tensor(0.0, device=cav_vectors.device)

        text_emb = self.text_embeddings.to(cav_vectors.device)
        text_norm = F.normalize(text_emb, dim=-1)
        cav_norm = cav_vectors / (cav_vectors.norm(dim=-1, keepdim=True) + 1e-8)

        if top_k_indices is not None:
            B = top_k_indices.shape[0]
            loss_total = 0.0
            for b in range(B):
                tk_idx = top_k_indices[b]
                cav_b = cav_norm[tk_idx]
                text_b = text_norm[tk_idx]
                sim = torch.matmul(cav_b, text_b.T) / self.tau
                labels_b = torch.arange(len(tk_idx), device=cav_vectors.device)
                loss_total += F.cross_entropy(sim, labels_b)
            loss_ce = loss_total / max(B, 1)
            cav_target_sim = F.cosine_similarity(cav_norm, text_norm, dim=-1)
            loss_norm = ((1 - cav_target_sim) ** 2).mean()
            loss = loss_ce + 1.0 * loss_norm
        else:
            similarity = torch.matmul(cav_norm, text_norm.T) / self.tau
            labels = torch.arange(len(cav_norm), device=cav_vectors.device)
            loss_ce = F.cross_entropy(similarity, labels)
            cav_target_sim = F.cosine_similarity(cav_norm, text_norm, dim=-1)
            loss_norm = ((1 - cav_target_sim) ** 2).mean()
            loss = loss_ce + 1.0 * loss_norm

        return loss

    def forward(
        self,
        disease_logits: torch.Tensor,
        concept_scores: torch.Tensor,
        labels: torch.Tensor,
        patch_similarity: torch.Tensor,
        patch_features: torch.Tensor,
        cav_vectors: torch.Tensor,
        w_vote: torch.Tensor = None,
        top_k_indices: torch.Tensor = None
    ) -> Dict[str, torch.Tensor]:
        """
        计算总损失

        L_total = L_cls + α·L_align + β·L_text

        参数:
            disease_logits: 疾病预测logits [B, 7]
            concept_scores: 概念激活分数 [B, 80]
            labels: 真实标签 [B]
            patch_similarity: patch-概念相似度 [B, 196, 80]
            patch_features: patch特征 [B, 196, 512]
            cav_vectors: CAV向量 [80, 512]
            w_vote: 疾病-概念投票矩阵 [7, 80]
            top_k_indices: Top-K概念的索引 [B, top_k]
        """
        loss_cls = self.focal_loss(disease_logits, labels)

        loss_align = torch.tensor(0.0, device=disease_logits.device)
        if patch_features is not None and patch_similarity is not None and w_vote is not None:
            loss_align = self.cross_view_alignment_loss(
                patch_features, cav_vectors, patch_similarity, labels, w_vote,
                concept_scores=concept_scores
            )

        loss_text = torch.tensor(0.0, device=disease_logits.device)
        if self.use_text_anchor and self.text_embeddings is not None:
            if top_k_indices is not None:
                B = top_k_indices.shape[0]
                text_emb = self.text_embeddings.to(cav_vectors.device)
                text_norm = F.normalize(text_emb, dim=-1)
                cav_norm = cav_vectors / (cav_vectors.norm(dim=-1, keepdim=True) + 1e-8)
                loss_text_total = 0.0
                for b in range(B):
                    tk_idx = top_k_indices[b]
                    cav_b = cav_norm[tk_idx]
                    text_b = text_norm[tk_idx]
                    sim = torch.matmul(cav_b, text_b.T) / self.tau
                    sim = torch.clamp(sim, min=-100, max=100)
                    labels_b = torch.arange(len(tk_idx), device=cav_vectors.device)
                    loss_text_total += F.cross_entropy(sim, labels_b)
                loss_text = loss_text_total / max(B, 1)
            else:
                loss_text = self.info_nce_loss(cav_vectors)

        total_loss = (
            loss_cls
            + self.alpha * loss_align
            + self.beta * loss_text
        )

        return {
            'total': total_loss,
            'classification': loss_cls,
            'alignment': loss_align,
            'text_anchor': loss_text
        }


def compute_class_separation(concept_scores: torch.Tensor, labels: torch.Tensor) -> float:
    """计算类间分离度"""
    num_classes = 7

    between_var = 0.0
    within_var = 0.0
    count = 0

    for c in range(concept_scores.size(1)):
        class_means = []
        for cls in range(num_classes):
            mask = (labels == cls)
            if mask.sum() > 0:
                class_means.append(concept_scores[mask, c].mean().item())

        if len(class_means) >= 2:
            between_var += torch.var(torch.tensor(class_means)).item()
            count += 1

    if count > 0:
        between_var /= count

    for cls in range(num_classes):
        mask = (labels == cls)
        if mask.sum() > 0:
            within_var += concept_scores[mask].var(dim=0).mean().item()

    separation = between_var / (within_var + 1e-8)
    return separation

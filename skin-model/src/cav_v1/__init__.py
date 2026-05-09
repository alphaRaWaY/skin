"""
CAV v1 - 基于Patch-概念余弦相似度的可解释模型

核心设计:
1. 每个patch token与所有80个CAV向量计算余弦相似度 → 196×80概念响应矩阵
2. Top-K池化: 对每个概念，取响应最高的K个patch聚合 → 图像级概念激活分数
3. 疾病投票: 概念得分与W_vote权重相乘 → 疾病预测
"""

from cav_v1.cav_model import CAVModelV1, create_cav_model, TopKPooling
from cav_v1.cav_loss import CAVLossV1, compute_class_separation

__all__ = [
    'CAVModelV1',
    'create_cav_model',
    'TopKPooling',
    'CAVLossV1',
    'compute_class_separation'
]

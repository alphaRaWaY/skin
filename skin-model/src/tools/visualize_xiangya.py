"""
局部证据匹配可视化 - 湘雅12分类 (论文级)
基于heatmap_viz_v8架构，适配ViLCBM 12分类融合模型
修复: 对比性证据→原始相似度, Top-1→Top-3加权融合, 过度后处理→精简, 概念选择加入w_vote
"""
import sys
import os
import json
import csv
import re
import argparse
from pathlib import Path
from typing import Dict, List, Tuple, Optional

import torch
import torch.nn.functional as F
import numpy as np
import cv2
from PIL import Image, ImageDraw, ImageFont
from torchvision import transforms
from tqdm import tqdm

viz_dir = Path(__file__).resolve().parent
project_root = viz_dir.parents[1]
src_root = project_root / "src"
sys.path.insert(0, str(project_root))
sys.path.insert(0, str(src_root))

from vilc_bm.cav_model import ViLCBM

LOCAL_DATA_ROOT = project_root / "config"
LOCAL_IMAGES_DIR = project_root / "data" / "examples"
LOCAL_PRETRAIN = project_root / "weights" / "panderm_backbone.pth"
LOCAL_MODEL_DIR = project_root / "weights"

IMAGE_SIZE = 224
FUSION_WEIGHTS = (0.3, 0.7)

DISEASE_INDEX_MAP = {
    "verruca_vulgaris": 0,
    "vitiligo": 1,
    "psoriasis": 2,
    "pityriasis_rosea": 3,
    "lichen_planus": 4,
    "eczema": 5,
    "acne_vulgaris": 6,
    "melanocytic_nevus": 7,
    "bowen_disease": 8,
    "basal_cell_carcinoma": 9,
    "squamous_cell_carcinoma": 10,
    "melanoma": 11,
}

DISEASE_NAMES_CN = {
    0: "寻常疣", 1: "白癜风", 2: "银屑病", 3: "玫瑰糠疹",
    4: "扁平苔藓", 5: "湿疹", 6: "寻常痤疮", 7: "色素痣",
    8: "鲍温病", 9: "基底细胞癌", 10: "鳞状细胞癌", 11: "黑色素瘤",
}

CONCEPT_COLORS_RGB = [
    (220, 50, 50), (50, 180, 50), (50, 100, 220),
    (220, 180, 50), (180, 50, 180), (50, 180, 180),
]

TRAIN_TRANSFORM = transforms.Compose([
    transforms.Resize((IMAGE_SIZE, IMAGE_SIZE), interpolation=transforms.InterpolationMode.BICUBIC),
    transforms.ToTensor(),
    transforms.Normalize(
        mean=[0.48145466, 0.4578275, 0.40821073],
        std=[0.26862954, 0.26130258, 0.27577711]
    ),
])


def get_disease_label_from_filename(filename: str) -> Optional[int]:
    pattern = r'湘雅_\d+([a-z][a-z_]*?)_xy'
    match = re.search(pattern, filename, re.IGNORECASE)
    if match:
        disease_name = match.group(1).lower()
        if disease_name in DISEASE_INDEX_MAP:
            return DISEASE_INDEX_MAP[disease_name]
    return None


def load_csv_images(csv_path: str) -> List[str]:
    filenames = []
    with open(csv_path, "r", encoding="utf-8-sig") as f:
        reader = csv.DictReader(f)
        fieldnames = reader.fieldnames
        if not fieldnames:
            return []
        filename_col = None
        for col in fieldnames:
            if "filename" in col.lower():
                filename_col = col
                break
        if not filename_col:
            return []
        for row in reader:
            filename = row.get(filename_col, "")
            if filename:
                filenames.append(filename.strip())
    return filenames


def load_test_set(split_path: str) -> List[Tuple[str, int]]:
    with open(split_path, "r", encoding="utf-8") as f:
        split_data = json.load(f)

    test_items = split_data.get("test", [])
    print(f"[Load] test set: {len(test_items)} 张图片 (from {split_path})")
    return [(item[0], int(item[1])) for item in test_items]


def load_all_sets(split_path: str) -> List[Tuple[str, int]]:
    with open(split_path, "r", encoding="utf-8") as f:
        split_data = json.load(f)

    train_items = split_data.get("train", [])
    test_items = split_data.get("test", [])
    all_items = train_items + test_items
    print(f"[Load] global set: {len(all_items)} 张图片 (from {split_path})")
    print(f"        train: {len(train_items)} + test: {len(test_items)}")
    return [(item[0], int(item[1])) for item in all_items]


class Xiangya12ClassVisualizer:
    def __init__(self, model1_path: str, model2_path: str, device: str = "cuda"):
        self.device = torch.device(device if torch.cuda.is_available() else "cpu")
        self._load_config()
        self._load_models(model1_path, model2_path)
        self._setup_transform()

    def _load_config(self):
        config_path = LOCAL_DATA_ROOT / "xiangya_12class.json"
        with open(config_path, "r", encoding="utf-8") as f:
            self.config = json.load(f)

        self.disease_list = self.config["diseases"]
        self.concept_list = self.config["concept_list"]
        self.disease_mapping = self.config.get("disease_concept_mapping", {})
        self.num_diseases = len(self.disease_list)
        self.num_concepts = len(self.concept_list)
        print(f"[Config] 疾病: {self.num_diseases}类, 概念: {self.num_concepts}个")

    def _load_models(self, model1_path: str, model2_path: str):
        def _load_single(path):
            model = ViLCBM(
                panderm_checkpoint=str(LOCAL_PRETRAIN),
                num_concepts=self.num_concepts,
                embed_dim=512,
                freeze_vit=True,
                unfreeze_layers=4,
                top_k=16,
                disease_mapping_path=str(LOCAL_DATA_ROOT / "xiangya_12class.json"),
                num_diseases=self.num_diseases,
            )
            ckpt = torch.load(path, map_location=self.device, weights_only=True)
            state_dict = ckpt["model_state_dict"] if "model_state_dict" in ckpt else ckpt
            filtered = {k: v for k, v in state_dict.items() if not k.startswith("visual_encoder.head.")}
            model.load_state_dict(filtered, strict=False)
            model.to(self.device)
            model.eval()
            return model

        self.model1 = _load_single(model1_path)
        self.model2 = _load_single(model2_path)
        print(f"[Model1] 加载成功: {model1_path}")
        print(f"[Model2] 加载成功: {model2_path}")

        self.w_vote = self.model1.w_vote.detach().cpu().numpy()
        self.concept_vectors = self.model1.concept_vectors.detach()

    def _setup_transform(self):
        self.image_transform = TRAIN_TRANSFORM

    def _find_image(self, filename: str) -> Optional[str]:
        filename_lower = filename.lower()
        if filename_lower.endswith((".jpg", ".jpeg", ".png")):
            path = LOCAL_IMAGES_DIR / filename
            if path.exists():
                return str(path)
        for ext in [".jpg", ".jpeg"]:
            path = LOCAL_IMAGES_DIR / f"{filename}{ext}"
            if path.exists():
                return str(path)
        return None

    def fusion_inference(self, image_tensor: torch.Tensor) -> Dict:
        with torch.no_grad():
            outputs1 = self.model1(image_tensor)
            outputs2 = self.model2(image_tensor)

        fused_logits = FUSION_WEIGHTS[0] * outputs1["disease_logits"] + FUSION_WEIGHTS[1] * outputs2["disease_logits"]
        fused_probs = F.softmax(fused_logits, dim=1)

        patch_similarity1 = outputs1["patch_similarity"]
        patch_similarity2 = outputs2["patch_similarity"]
        concept_scores1 = outputs1["concept_scores"]
        concept_scores2 = outputs2["concept_scores"]

        fused_patch_similarity = FUSION_WEIGHTS[0] * patch_similarity1 + FUSION_WEIGHTS[1] * patch_similarity2
        fused_concept_scores = FUSION_WEIGHTS[0] * concept_scores1 + FUSION_WEIGHTS[1] * concept_scores2

        pred_label = fused_probs.argmax(dim=1).item()
        pred_confidence = fused_probs.max(dim=1)[0].item()

        return {
            "pred_label": pred_label,
            "pred_confidence": pred_confidence,
            "fused_probs": fused_probs.cpu().numpy()[0],
            "patch_similarity": fused_patch_similarity.cpu().numpy()[0],
            "concept_scores": fused_concept_scores.cpu().numpy()[0],
        }

    def compute_concept_heatmaps(self, patch_similarity: np.ndarray, top_ratio: float = 0.30) -> np.ndarray:
        if patch_similarity.ndim == 2:
            patch_similarity = patch_similarity[None, ...]

        B, num_patches, C = patch_similarity.shape
        heatmap_all = np.zeros((B, C, num_patches), dtype=np.float32)

        for b in range(B):
            for c in range(C):
                evidence = np.maximum(patch_similarity[b, :, c], 0)
                e_max = evidence.max()
                if e_max < 1e-8:
                    continue
                k = max(1, int(num_patches * top_ratio))
                flat = evidence.flatten()
                threshold = np.partition(flat, -k)[-k]
                evidence[evidence < threshold] = 0
                e_new_max = evidence.max()
                if e_new_max > 1e-8:
                    evidence = evidence / e_new_max
                heatmap_all[b, c, :] = evidence

        return heatmap_all

    def create_heatmap_overlay(self, img_array: np.ndarray, heatmap_2d: np.ndarray, alpha: float = 0.5) -> np.ndarray:
        img_h, img_w = img_array.shape[:2]

        heatmap = cv2.resize(heatmap_2d.astype(np.float32), (img_w, img_h), interpolation=cv2.INTER_CUBIC)
        heatmap = cv2.bilateralFilter(heatmap, d=9, sigmaColor=75, sigmaSpace=75)

        h_min, h_max = heatmap.min(), heatmap.max()
        if h_max - h_min > 1e-8:
            heatmap = (heatmap - h_min) / (h_max - h_min)
        else:
            heatmap = np.zeros_like(heatmap)

        heatmap_enhanced = np.power(heatmap, 0.7)

        heatmap_color = cv2.applyColorMap(
            np.uint8(255 * heatmap_enhanced),
            cv2.COLORMAP_INFERNO
        )
        heatmap_color = cv2.cvtColor(heatmap_color, cv2.COLOR_BGR2RGB)

        overlay = cv2.addWeighted(img_array, 1 - alpha, heatmap_color, alpha, 0)

        threshold = float(heatmap.mean() + heatmap.std())
        threshold = min(max(threshold, 0.3), 0.7)
        mask = (heatmap > threshold).astype(np.uint8)
        kernel = np.ones((3, 3), np.uint8)
        mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, kernel)
        contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        cv2.drawContours(overlay, contours, -1, (255, 255, 255), 2)

        return overlay

    def detect_lesion_mask(self, img_array: np.ndarray) -> np.ndarray:
        """鲁棒性皮损区域检测 - 基于LAB颜色空间A通道"""
        lab = cv2.cvtColor(img_array, cv2.COLOR_RGB2LAB)
        _, a_channel, _ = cv2.split(lab)

        blurred_a = cv2.GaussianBlur(a_channel, (5, 5), 0)
        _, binary = cv2.threshold(blurred_a, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)

        kernel_close = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (15, 15))
        kernel_open = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (7, 7))
        binary = cv2.morphologyEx(binary, cv2.MORPH_CLOSE, kernel_close)
        binary = cv2.morphologyEx(binary, cv2.MORPH_OPEN, kernel_open)

        contours, _ = cv2.findContours(binary, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        mask = np.zeros_like(a_channel, dtype=np.uint8)

        if contours:
            largest_contour = max(contours, key=cv2.contourArea)
            cv2.drawContours(mask, [largest_contour], -1, 255, -1)

            mask_area_ratio = np.sum(mask == 255) / (mask.shape[0] * mask.shape[1])
            if mask_area_ratio < 0.05 or mask_area_ratio > 0.95:
                mask = np.ones_like(a_channel, dtype=np.uint8) * 255
        else:
            mask = np.ones_like(a_channel, dtype=np.uint8) * 255

        return mask.astype(np.float32) / 255.0

    def crop_dermoscopy_circle(self, img_array: np.ndarray) -> np.ndarray:
        if len(img_array.shape) == 3:
            gray = cv2.cvtColor(img_array, cv2.COLOR_RGB2GRAY)
        else:
            gray = img_array

        _, binary = cv2.threshold(gray, 10, 255, cv2.THRESH_BINARY)
        contours, _ = cv2.findContours(binary, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        if not contours:
            return img_array

        largest_contour = max(contours, key=cv2.contourArea)
        x, y, w, h = cv2.boundingRect(largest_contour)
        return img_array[y:y+h, x:x+w]

    def generate_single_viz(
        self,
        image_path: str,
        true_label: Optional[int] = None,
        top_k: int = 3,
        alpha: float = 0.5,
        apply_lesion_mask: bool = False,
    ) -> Tuple[Image.Image, Dict]:
        pil_image = Image.open(image_path).convert("RGB")
        img_original = np.array(pil_image)
        img_original = self.crop_dermoscopy_circle(img_original)

        # 皮损区域检测 - 病灶先验
        lesion_mask = self.detect_lesion_mask(img_original)

        image_tensor = self.image_transform(pil_image).unsqueeze(0).to(self.device)
        outputs = self.fusion_inference(image_tensor)

        pred_label = outputs["pred_label"]
        pred_confidence = outputs["pred_confidence"]
        patch_similarity = outputs["patch_similarity"]
        concept_scores = outputs["concept_scores"]

        heatmaps_all = self.compute_concept_heatmaps(patch_similarity)

        # 获取网格尺寸
        grid_size = int(np.sqrt(patch_similarity.shape[0]))

        # 直接用heatmap激活强度选Top-3概念（不依赖w_vote，因为w_vote可能全为0）
        # 取pred_label对应概念的平均激活强度作为排序依据
        w_vote_pred = self.w_vote[pred_label]
        if np.any(w_vote_pred > 0):
            contribution_scores = concept_scores * w_vote_pred
            contribution_scores = np.maximum(contribution_scores, 0)
        else:
            # w_vote全为0时，直接用pred_label对应概念的平均激活
            pred_concept_avg = heatmaps_all[0, :, :].mean(axis=1)  # [num_concepts]
            contribution_scores = pred_concept_avg

        sorted_idx = np.argsort(contribution_scores)[::-1]

        # Top-3概念融合：按贡献度加权平均
        top_k_indices = sorted_idx[:top_k]
        top_k_heatmaps = []
        top_k_weights = []
        for ci in top_k_indices:
            flat_activation = heatmaps_all[0, ci, :]
            hm_2d = flat_activation.reshape(grid_size, grid_size)
            top_k_heatmaps.append(hm_2d)
            top_k_weights.append(contribution_scores[ci])

        # 归一化权重
        total_weight = sum(top_k_weights)
        if total_weight > 1e-8:
            top_k_heatmaps = [h * (w / total_weight) for h, w in zip(top_k_heatmaps, top_k_weights)]
            fused_heatmap_flat = sum(top_k_heatmaps)
        else:
            fused_heatmap_flat = top_k_heatmaps[0]

        # 病灶先验过滤：抑制背景激活
        fused_heatmap_2d = fused_heatmap_flat.reshape(grid_size, grid_size)

        # 上采样到原图尺寸并应用病灶掩码
        fused_heatmap_resized = cv2.resize(fused_heatmap_2d, (img_original.shape[1], img_original.shape[0]), interpolation=cv2.INTER_CUBIC)
        if apply_lesion_mask:
            fused_heatmap_resized = fused_heatmap_resized * lesion_mask

        # 生成热力图叠加
        overlay_combined = self.create_heatmap_overlay(img_original, fused_heatmap_resized, alpha)

        is_correct = (pred_label == true_label) if true_label is not None else None

        top_k_concepts = []
        for i in range(min(top_k, len(sorted_idx))):
            ci = sorted_idx[i]
            concept_name = self.concept_list[ci]
            w = float(w_vote_pred[ci])
            score = float(concept_scores[ci])
            contribution = float(contribution_scores[ci])

            flat_activation = heatmaps_all[0, ci, :]
            hm_2d = flat_activation.reshape(grid_size, grid_size)
            hm_resized = cv2.resize(hm_2d.astype(np.float32), (64, 64), interpolation=cv2.INTER_CUBIC)
            hm_resized = cv2.GaussianBlur(hm_resized, (7, 7), 1.5)
            hm_min, hm_max = hm_resized.min(), hm_resized.max()
            if hm_max - hm_min > 1e-8:
                hm_resized = (hm_resized - hm_min) / (hm_max - hm_min)
            hm_color = cv2.applyColorMap((hm_resized * 255).astype(np.uint8), cv2.COLORMAP_INFERNO)
            hm_color = cv2.cvtColor(hm_color, cv2.COLOR_BGR2RGB)

            top_k_concepts.append({
                "index": int(ci),
                "name": concept_name,
                "score": score,
                "weight": w,
                "contribution": contribution,
                "heatmap": hm_color,
            })

        viz_image = self._create_visualization_image(
            img_original, overlay_combined,
            pred_label, pred_confidence, true_label, is_correct,
            top_k_concepts, alpha,
        )

        result = {
            "image_path": image_path,
            "true_label": true_label,
            "pred_label": pred_label,
            "pred_disease": self.disease_list[pred_label],
            "confidence": pred_confidence,
            "is_correct": is_correct,
            "top_k_concepts": top_k_concepts,
            "fusion_probs": outputs["fused_probs"],
        }

        return viz_image, result

    def _create_visualization_image(
        self,
        img_original: np.ndarray,
        heatmap_overlay: np.ndarray,
        pred_label: int,
        pred_confidence: float,
        true_label: Optional[int],
        is_correct: Optional[bool],
        top_k_concepts: List[Dict],
        alpha: float,
    ) -> Image.Image:
        import matplotlib
        matplotlib.use('Agg')
        import matplotlib.pyplot as plt
        from matplotlib.colors import LinearSegmentedColormap

        plt.rcParams['axes.unicode_minus'] = False
        for fn in ['SimHei', 'Microsoft YaHei', 'Arial Unicode MS']:
            try:
                plt.rcParams['font.sans-serif'] = [fn] + plt.rcParams['font.sans-serif']
                plt.rcParams['font.family'] = 'sans-serif'
                break
            except:
                continue

        pred_disease = self.disease_list[pred_label]
        pred_cn = DISEASE_NAMES_CN.get(pred_label, pred_disease)
        true_cn = DISEASE_NAMES_CN.get(true_label, "未知") if true_label is not None else "未知"

        if is_correct is not None:
            mark = "OK" if is_correct else "NO"
            mark_color = "#27ae60" if is_correct else "#e74c3c"
        else:
            mark = ""
            mark_color = "#34495e"

        fig = plt.figure(figsize=(16, 10))
        fig.set_facecolor('white')

        fig.suptitle("局部证据匹配可视化 - 湘雅12分类", fontsize=18, fontweight='bold', y=0.98, color='#2c3e50')

        prediction_text = f"真实: {true_cn} | 预测: {pred_cn} ({pred_confidence:.1%}) {mark}"
        plt.figtext(0.5, 0.92, prediction_text, fontsize=13, ha='center', color=mark_color, fontweight='bold')

        gs = fig.add_gridspec(2, 3, height_ratios=[3, 1], width_ratios=[1, 1, 0.05],
                              hspace=0.15, wspace=0.08, left=0.03, right=0.97, top=0.88, bottom=0.08)

        ax1 = fig.add_subplot(gs[0, 0])
        ax1.imshow(img_original)
        ax1.set_title("原始图像", fontsize=13, fontweight='bold', pad=6, color='#2c3e50')
        ax1.axis('off')

        ax2 = fig.add_subplot(gs[0, 1])
        ax2.imshow(heatmap_overlay)
        if is_correct is not None:
            ax2.text(0.98, 0.98, mark, transform=ax2.transAxes, fontsize=18, fontweight='bold',
                     color='white', ha='right', va='top',
                     bbox=dict(boxstyle='round,pad=0.4', facecolor=mark_color, edgecolor='white', linewidth=2))
        ax2.set_title("证据热力图 (Top-3融合)", fontsize=13, fontweight='bold', pad=6, color='#2c3e50')
        ax2.axis('off')

        ax_cbar = fig.add_subplot(gs[0, 2])
        sm = plt.cm.ScalarMappable(cmap='inferno', norm=plt.Normalize(0, 1))
        sm.set_array([])
        cbar = plt.colorbar(sm, cax=ax_cbar)
        cbar.set_label('激活强度', fontsize=11, fontweight='bold')
        cbar.ax.tick_params(labelsize=9)

        ax_table = fig.add_subplot(gs[1, :])
        ax_table.axis('off')

        table_data = []
        cell_colors = []
        for i, concept in enumerate(top_k_concepts):
            color_rgb = CONCEPT_COLORS_RGB[i % len(CONCEPT_COLORS_RGB)]
            color_hex = '#{:02x}{:02x}{:02x}'.format(*color_rgb)

            table_data.append([
                "",
                f"{i+1}",
                concept['name'],
                f"{concept['score']:.3f}",
                f"{concept['weight']:.1f}",
                f"{concept['contribution']:.3f}",
            ])
            row_colors = ['#f8f9fa'] * 6
            row_colors[0] = color_hex
            cell_colors.append(row_colors)

        table = ax_table.table(
            cellText=table_data,
            colLabels=['颜色', '序号', '概念名称', '激活值', 'w_vote', '贡献度'],
            cellLoc='center',
            loc='center',
            colWidths=[0.06, 0.06, 0.35, 0.12, 0.12, 0.12],
            cellColours=cell_colors,
        )
        table.auto_set_font_size(False)
        table.set_fontsize(10)
        table.scale(1, 1.8)

        for j in range(6):
            cell = table[(0, j)]
            cell.set_facecolor('#3498db')
            cell.set_text_props(color='white', fontweight='bold', fontsize=11)
            cell.set_height(0.15)

        for i in range(1, len(table_data) + 1):
            for j in range(6):
                cell = table[(i, j)]
                if j == 0:
                    cell.set_text_props(color='white', fontweight='bold', fontsize=14)
                elif j == 2:
                    cell.set_text_props(fontweight='bold', color='#2c3e50')
                elif j in [3, 5]:
                    cell.set_text_props(fontweight='bold', color='#27ae60')
                else:
                    cell.set_text_props(color='#34495e')
                cell.set_height(0.12)

        fig.canvas.draw()
        buf = fig.canvas.buffer_rgba()
        w_canvas, h_canvas = fig.canvas.get_width_height()
        img_array = np.frombuffer(buf, dtype=np.uint8).reshape(h_canvas, w_canvas, 4)
        plt.close(fig)

        return Image.fromarray(img_array[:, :, :3])


def main():
    parser = argparse.ArgumentParser(description="局部证据匹配可视化 - 湘雅12分类")
    parser.add_argument("--output_dir", type=str, default=str(Path(project_root) / "output" / "xiangya_12class_viz"))
    parser.add_argument("--top_k", type=int, default=3)
    parser.add_argument("--alpha", type=float, default=0.5)
    parser.add_argument("--model1", type=str, default=str(LOCAL_MODEL_DIR / "xiangya_model_primary.pt"))
    parser.add_argument("--model2", type=str, default=str(LOCAL_MODEL_DIR / "xiangya_model_secondary.pt"))
    parser.add_argument("--device", type=str, default="cuda")
    parser.add_argument("--image", type=str, default=None,
                        help="single image path; bypass dataset batch mode")
    parser.add_argument("--true_label", type=int, default=None,
                        help="optional true disease index for a single image")
    parser.add_argument("--lesion_mask", action="store_true",
                        help="enable the experimental LAB lesion mask")
    parser.add_argument("--data_source", type=str, default="questionnaire",
                        choices=["questionnaire", "test", "all"],
                        help="数据源: questionnaire(192张问卷)/test(测试集)/all(全局训练+测试)")
    parser.add_argument("--split_path", type=str,
                        default=r"D:\Project\PanDerm\dermatology_data\湘雅皮肤镜数据集_12class\split.json",
                        help="split.json 路径")
    parser.add_argument("--limit", type=int, default=0,
                        help="限制处理图片数量, 0 表示不限制")
    parser.add_argument("--filter_disease", type=str, default=None,
                        help="按疾病类型过滤, 例如: melanoma, basal_cell_carcinoma 等")
    args = parser.parse_args()

    os.makedirs(args.output_dir, exist_ok=True)

    if args.image:
        visualizer = Xiangya12ClassVisualizer(args.model1, args.model2, args.device)
        viz_image, result = visualizer.generate_single_viz(
            args.image,
            true_label=(
                args.true_label
                if args.true_label is not None
                else get_disease_label_from_filename(Path(args.image).name)
            ),
            top_k=args.top_k,
            alpha=args.alpha,
            apply_lesion_mask=args.lesion_mask,
        )
        output_path = Path(args.output_dir) / f"{Path(args.image).stem}_visualization.png"
        viz_image.save(output_path)
        print(f"[Done] {output_path}")
        print(
            f"[Prediction] {result['pred_disease']} "
            f"({result['confidence']:.2%})"
        )
        return

    if args.data_source == "questionnaire":
        grouping_dir = Path(r"D:/Project/PanDerm/热力图192分组")
        versions = ["A", "B", "C", "D"]
        all_filenames = []

        for version in versions:
            csv_path = grouping_dir / f"questionnaire_version_{version}.csv"
            if not csv_path.exists():
                print(f"[Warning] CSV文件不存在: {csv_path}")
                continue
            filenames = load_csv_images(str(csv_path))
            print(f"[Load] version_{version}: {len(filenames)} 张图片")
            all_filenames.extend(filenames)

        if not all_filenames:
            print("[Error] 没有找到任何图片数据")
            return

        print(f"\n[总览] 共加载 {len(all_filenames)} 张图片 (questionnaire)")
        image_label_pairs = None
    else:
        if not Path(args.split_path).exists():
            print(f"[Error] split.json 不存在: {args.split_path}")
            return

        if args.data_source == "test":
            image_label_pairs = load_test_set(args.split_path)
            if not image_label_pairs:
                print("[Error] 测试集为空")
                return
            print(f"\n[总览] 共加载 {len(image_label_pairs)} 张图片 (test set)")
        else:
            image_label_pairs = load_all_sets(args.split_path)
            if not image_label_pairs:
                print("[Error] 全局数据集为空")
                return
            print(f"\n[总览] 共加载 {len(image_label_pairs)} 张图片 (global: train + test)")

        all_filenames = [item[0] for item in image_label_pairs]

    if args.limit > 0:
        all_filenames = all_filenames[:args.limit]
        if image_label_pairs is not None:
            image_label_pairs = image_label_pairs[:args.limit]
        print(f"[Limit] 仅处理前 {args.limit} 张图片")

    if args.filter_disease:
        if args.filter_disease not in DISEASE_INDEX_MAP:
            print(f"[Error] 未知疾病类型: {args.filter_disease}")
            print(f"[可用疾病类型] {list(DISEASE_INDEX_MAP.keys())}")
            return
        target_label = DISEASE_INDEX_MAP[args.filter_disease]
        target_cn = DISEASE_NAMES_CN.get(target_label, args.filter_disease)
        print(f"\n[过滤] 仅处理 '{target_cn}' ({args.filter_disease}) 类型图片")

        if image_label_pairs is not None:
            filtered_pairs = [(fname, lbl) for fname, lbl in image_label_pairs if lbl == target_label]
            image_label_pairs = filtered_pairs
            all_filenames = [item[0] for item in image_label_pairs]
            print(f"[过滤后] {len(image_label_pairs)} 张图片")
        else:
            filtered_filenames = []
            for fname in all_filenames:
                lbl = get_disease_label_from_filename(fname)
                if lbl == target_label:
                    filtered_filenames.append(fname)
            all_filenames = filtered_filenames
            print(f"[过滤后] {len(all_filenames)} 张图片")

        args.output_dir = os.path.join(os.path.dirname(args.output_dir),
                                       os.path.basename(args.output_dir) + f"_{args.filter_disease}")
        print(f"[输出] 专用目录: {args.output_dir}")
        os.makedirs(args.output_dir, exist_ok=True)

    print(f"\n[初始化] 加载模型...")
    visualizer = Xiangya12ClassVisualizer(args.model1, args.model2, args.device)
    print(f"[Init] 设备: {visualizer.device}")

    label_from_split = {}
    if image_label_pairs is not None:
        for fname, lbl in image_label_pairs:
            label_from_split[fname] = lbl

    results = []
    for filename in tqdm(all_filenames, desc="处理"):
        image_path = visualizer._find_image(filename)
        if image_path is None:
            print(f"[Warning] 图片不存在: {filename}")
            continue

        if filename in label_from_split:
            true_label = label_from_split[filename]
        else:
            true_label = get_disease_label_from_filename(filename)

        try:
            viz_image, result = visualizer.generate_single_viz(
                image_path,
                true_label=true_label,
                top_k=args.top_k,
                alpha=args.alpha,
                apply_lesion_mask=args.lesion_mask,
            )

            base_name = os.path.splitext(filename)[0]
            output_path = os.path.join(args.output_dir, f"{base_name}.png")
            viz_image.save(output_path)
            results.append(result)

        except Exception as e:
            print(f"[Error] 处理失败: {filename} - {e}")

    print(f"\n{'='*60}")
    print(f"[融合模型评估 {FUSION_WEIGHTS[0]:.1f}:{FUSION_WEIGHTS[1]:.1f}] 完成 {len(results)}/{len(all_filenames)} 张图片")
    print(f"{'='*60}")

    if results:
        correct = sum(1 for r in results if r["is_correct"])
        avg_conf = np.mean([r["confidence"] for r in results])
        print(f"\nTop-1 正确率: {correct}/{len(results)} = {correct/len(results):.2%}")
        print(f"平均置信度: {avg_conf:.1%}")

        disease_stats = {}
        for r in results:
            true_lb = r["true_label"]
            if true_lb is not None:
                disease_name = visualizer.disease_list[true_lb]
                if disease_name not in disease_stats:
                    disease_stats[disease_name] = {"total": 0, "correct": 0, "top3_correct": 0}
                disease_stats[disease_name]["total"] += 1
                if r["is_correct"]:
                    disease_stats[disease_name]["correct"] += 1
                fused_probs = r["fusion_probs"]
                top3_labels = np.argsort(fused_probs)[::-1][:3]
                if true_lb in top3_labels:
                    disease_stats[disease_name]["top3_correct"] += 1

        if disease_stats:
            print(f"\n{'='*60}")
            print(f"[分类统计] Top-1 / Top-3")
            print(f"{'='*60}")
            print(f"{'疾病类型':<30} {'总数':>6} {'Top1':>6} {'Top1率':>8} {'Top3':>6} {'Top3率':>8}")
            print(f"{'-'*60}")
            for disease in sorted(disease_stats.keys()):
                stats = disease_stats[disease]
                top1_rate = stats["correct"] / stats["total"] if stats["total"] > 0 else 0
                top3_rate = stats["top3_correct"] / stats["total"] if stats["total"] > 0 else 0
                print(f"{disease:<30} {stats['total']:>6} {stats['correct']:>6} {top1_rate:>7.1%} {stats['top3_correct']:>6} {top3_rate:>7.1%}")

        top3_total = sum(1 for r in results if r["true_label"] is not None)
        top3_correct = 0
        for r in results:
            if r["true_label"] is not None:
                top3_labels = np.argsort(r["fusion_probs"])[::-1][:3]
                if r["true_label"] in top3_labels:
                    top3_correct += 1

        print(f"\n[整体统计]")
        print(f"  Top-1 正确率: {correct}/{top3_total} = {correct/top3_total:.2%}")
        print(f"  Top-3 正确率: {top3_correct}/{top3_total} = {top3_correct/top3_total:.2%}")

    print(f"\n[完成] 输出目录: {args.output_dir}")


if __name__ == "__main__":
    main()

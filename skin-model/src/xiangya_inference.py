import base64
import io
import json
import math
from pathlib import Path
from typing import Any

import cv2
import numpy as np
import torch
import torch.nn.functional as F
from PIL import Image
from torchvision import transforms

from vilc_bm.cav_model import ViLCBM


FUSION_WEIGHTS = (0.3, 0.7)
IMAGE_TRANSFORM = transforms.Compose(
    [
        transforms.Resize((224, 224), interpolation=transforms.InterpolationMode.BICUBIC),
        transforms.ToTensor(),
        transforms.Normalize(
            mean=[0.48145466, 0.4578275, 0.40821073],
            std=[0.26862954, 0.26130258, 0.27577711],
        ),
    ]
)


class Xiangya12ClassInferenceModel:
    """Flask inference adapter for the original Xiangya dual-model pipeline."""

    model_name = "xiangya-12class-dual"

    def __init__(
        self,
        model1_path: Path,
        model2_path: Path,
        config_path: Path,
        backbone_path: Path,
        device: str,
    ):
        self.device = torch.device(
            device if device != "cuda" or torch.cuda.is_available() else "cpu"
        )
        with config_path.open("r", encoding="utf-8") as file:
            self.config = json.load(file)

        self.diseases = self.config["diseases"]
        self.concepts = self.config["concept_list"]
        self.model1 = self._load_model(
            model1_path, config_path, backbone_path
        )
        self.model2 = self._load_model(
            model2_path, config_path, backbone_path
        )
        self.voting_weights = self.model1.w_vote.detach().cpu().numpy()

    def _load_model(
        self,
        checkpoint_path: Path,
        config_path: Path,
        backbone_path: Path,
    ):
        model = ViLCBM(
            panderm_checkpoint=str(backbone_path),
            num_concepts=len(self.concepts),
            embed_dim=512,
            freeze_vit=True,
            unfreeze_layers=4,
            top_k=16,
            disease_mapping_path=str(config_path),
            num_diseases=len(self.diseases),
        )
        checkpoint = torch.load(
            checkpoint_path, map_location=self.device, weights_only=False
        )
        state = checkpoint.get("model_state_dict", checkpoint)
        state = {
            key: value
            for key, value in state.items()
            if not key.startswith("visual_encoder.head.")
        }
        model.load_state_dict(state, strict=False)
        model.to(self.device)
        model.eval()
        return model

    def predict(
        self,
        image: Image.Image,
        top_k: int = 16,
        heatmap_top_k: int = 3,
        heatmap_alpha: float = 0.5,
        heatmap_max_size: int = 768,
    ) -> dict[str, Any]:
        tensor = IMAGE_TRANSFORM(image).unsqueeze(0).to(self.device)
        with torch.inference_mode():
            output1 = self.model1(tensor)
            output2 = self.model2(tensor)

        disease_logits = (
            FUSION_WEIGHTS[0] * output1["disease_logits"]
            + FUSION_WEIGHTS[1] * output2["disease_logits"]
        )
        probabilities = F.softmax(disease_logits, dim=1)[0]
        patch_similarity = (
            FUSION_WEIGHTS[0] * output1["patch_similarity"]
            + FUSION_WEIGHTS[1] * output2["patch_similarity"]
        )[0]
        concept_scores = (
            FUSION_WEIGHTS[0] * output1["concept_scores"]
            + FUSION_WEIGHTS[1] * output2["concept_scores"]
        )[0]

        predicted_index = int(probabilities.argmax().item())
        vote_values = self.voting_weights[predicted_index]
        score_values = concept_scores.detach().cpu().numpy()
        heatmaps = self._compute_concept_heatmaps(
            patch_similarity.detach().cpu().numpy()
        )

        if np.any(vote_values > 0):
            contribution = np.maximum(score_values * vote_values, 0)
        else:
            contribution = heatmaps.mean(axis=1)
        contribution_order = np.argsort(contribution)[::-1]

        heatmap_count = max(1, min(heatmap_top_k, len(self.concepts)))
        heatmap_indices = [
            int(index) for index in contribution_order[:heatmap_count]
        ]
        heatmap_data = self._render_heatmap(
            image,
            heatmaps,
            contribution,
            heatmap_indices,
            heatmap_alpha,
            heatmap_max_size,
        )

        top_k = max(1, min(top_k, len(self.concepts)))
        score_order = np.argsort(score_values)[::-1][:top_k]
        concepts = [
            self._concept_result(
                int(index),
                rank,
                score_values,
                vote_values,
                contribution,
            )
            for rank, index in enumerate(score_order, start=1)
        ]
        heatmap_concepts = [
            self._concept_result(
                index,
                rank,
                score_values,
                vote_values,
                contribution,
            )
            for rank, index in enumerate(heatmap_indices, start=1)
        ]

        return {
            "diseaseIndex": predicted_index,
            "diseaseType": self.diseases[predicted_index],
            "diseaseNameEn": self.diseases[predicted_index],
            "diseaseNameCn": "",
            "confidence": float(probabilities[predicted_index].item()),
            "diseaseProbabilities": [
                {
                    "diseaseIndex": index,
                    "diseaseType": disease,
                    "diseaseNameEn": disease,
                    "diseaseNameCn": "",
                    "probability": float(probabilities[index].item()),
                }
                for index, disease in enumerate(self.diseases)
            ],
            "topKIndices": [item["index"] for item in concepts],
            "topKScores": [item["score"] for item in concepts],
            "concepts": concepts,
            "heatmap": {
                "mimeType": "image/png",
                "encoding": "base64",
                "data": heatmap_data,
                "conceptIndices": heatmap_indices,
                "concepts": heatmap_concepts,
                "patchGrid": [14, 14],
                "alpha": heatmap_alpha,
                "maxSize": heatmap_max_size,
            },
            "model": {
                "name": "xiangya-12class-dual",
                "diseaseCount": len(self.diseases),
                "conceptCount": len(self.concepts),
                "fusionWeights": list(FUSION_WEIGHTS),
            },
        }

    def _concept_result(
        self,
        index: int,
        rank: int,
        scores: np.ndarray,
        votes: np.ndarray,
        contributions: np.ndarray,
    ) -> dict[str, Any]:
        return {
            "index": index,
            "id": f"C{index:03d}",
            "nameEn": self.concepts[index],
            "nameCn": "",
            "category": "",
            "rank": rank,
            "score": float(scores[index]),
            "weight": float(votes[index]),
            "contribution": float(contributions[index]),
        }

    @staticmethod
    def _compute_concept_heatmaps(
        patch_similarity: np.ndarray, top_ratio: float = 0.30
    ) -> np.ndarray:
        num_patches, num_concepts = patch_similarity.shape
        result = np.zeros((num_concepts, num_patches), dtype=np.float32)
        top_count = max(1, int(num_patches * top_ratio))
        for concept_index in range(num_concepts):
            evidence = np.maximum(patch_similarity[:, concept_index], 0)
            if evidence.max() < 1e-8:
                continue
            threshold = np.partition(evidence, -top_count)[-top_count]
            evidence[evidence < threshold] = 0
            maximum = evidence.max()
            if maximum > 1e-8:
                evidence = evidence / maximum
            result[concept_index] = evidence
        return result

    def _render_heatmap(
        self,
        image: Image.Image,
        heatmaps: np.ndarray,
        contribution: np.ndarray,
        indices: list[int],
        alpha: float,
        max_size: int,
    ) -> str:
        weights = contribution[indices]
        total_weight = float(weights.sum())
        if total_weight > 1e-8:
            fused = sum(
                heatmaps[index] * (weight / total_weight)
                for index, weight in zip(indices, weights)
            )
        else:
            fused = heatmaps[indices[0]]

        grid_size = math.isqrt(fused.size)
        if grid_size * grid_size != fused.size:
            raise ValueError(f"Patch count {fused.size} is not a square grid")

        source = self._crop_dermoscopy_circle(np.asarray(image.convert("RGB")))
        fused = cv2.resize(
            fused.reshape(grid_size, grid_size),
            (source.shape[1], source.shape[0]),
            interpolation=cv2.INTER_CUBIC,
        )
        overlay = self._create_heatmap_overlay(source, fused, alpha)

        if max(overlay.shape[:2]) > max_size:
            scale = max_size / max(overlay.shape[:2])
            overlay = cv2.resize(
                overlay,
                (
                    max(1, round(overlay.shape[1] * scale)),
                    max(1, round(overlay.shape[0] * scale)),
                ),
                interpolation=cv2.INTER_AREA,
            )

        output = io.BytesIO()
        Image.fromarray(overlay).save(output, format="PNG", optimize=True)
        return base64.b64encode(output.getvalue()).decode("ascii")

    @staticmethod
    def _create_heatmap_overlay(
        source: np.ndarray, heatmap_2d: np.ndarray, alpha: float
    ) -> np.ndarray:
        heatmap = cv2.resize(
            heatmap_2d.astype(np.float32),
            (source.shape[1], source.shape[0]),
            interpolation=cv2.INTER_CUBIC,
        )
        heatmap = cv2.bilateralFilter(heatmap, d=9, sigmaColor=75, sigmaSpace=75)
        minimum, maximum = float(heatmap.min()), float(heatmap.max())
        heatmap = (
            (heatmap - minimum) / (maximum - minimum)
            if maximum - minimum > 1e-8
            else np.zeros_like(heatmap)
        )
        color = cv2.applyColorMap(
            np.uint8(255 * np.power(heatmap, 0.7)), cv2.COLORMAP_INFERNO
        )
        color = cv2.cvtColor(color, cv2.COLOR_BGR2RGB)
        overlay = cv2.addWeighted(source, 1 - alpha, color, alpha, 0)

        threshold = min(max(float(heatmap.mean() + heatmap.std()), 0.3), 0.7)
        mask = (heatmap > threshold).astype(np.uint8)
        mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, np.ones((3, 3), np.uint8))
        contours, _ = cv2.findContours(
            mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE
        )
        cv2.drawContours(overlay, contours, -1, (255, 255, 255), 2)
        return overlay

    @staticmethod
    def _detect_lesion_mask(source: np.ndarray) -> np.ndarray:
        _, a_channel, _ = cv2.split(cv2.cvtColor(source, cv2.COLOR_RGB2LAB))
        blurred = cv2.GaussianBlur(a_channel, (5, 5), 0)
        _, binary = cv2.threshold(
            blurred, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU
        )
        binary = cv2.morphologyEx(
            binary,
            cv2.MORPH_CLOSE,
            cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (15, 15)),
        )
        binary = cv2.morphologyEx(
            binary,
            cv2.MORPH_OPEN,
            cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (7, 7)),
        )
        contours, _ = cv2.findContours(
            binary, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE
        )
        mask = np.zeros_like(a_channel, dtype=np.uint8)
        if contours:
            cv2.drawContours(
                mask, [max(contours, key=cv2.contourArea)], -1, 255, -1
            )
            ratio = np.count_nonzero(mask == 255) / mask.size
            if ratio < 0.05 or ratio > 0.95:
                mask.fill(255)
        else:
            mask.fill(255)
        return mask.astype(np.float32) / 255.0

    @staticmethod
    def _crop_dermoscopy_circle(source: np.ndarray) -> np.ndarray:
        gray = cv2.cvtColor(source, cv2.COLOR_RGB2GRAY)
        _, binary = cv2.threshold(gray, 10, 255, cv2.THRESH_BINARY)
        contours, _ = cv2.findContours(
            binary, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE
        )
        if not contours:
            return source
        x, y, width, height = cv2.boundingRect(max(contours, key=cv2.contourArea))
        return source[y : y + height, x : x + width]

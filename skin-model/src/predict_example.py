"""
Skin model single-image inference example.

Supports dynamic model path configuration through:
1) CLI arguments
2) Environment variables
"""

import argparse
import json
import os
import sys
from pathlib import Path

import torch
from PIL import Image
from torchvision import transforms

sys.path.insert(0, str(Path(__file__).parent))
from cav_v1.cav_model import create_cav_model


DISEASE_NAMES = {
    0: "melanoma",
    1: "melanocytic_nevus",
    2: "basal_cell_carcinoma",
    3: "squamous_cell_carcinoma",
    4: "bowen_disease",
    5: "seborrheic_keratosis",
    6: "lichen_planus",
}

TRANSFORM = transforms.Compose(
    [
        transforms.Resize((224, 224)),
        transforms.ToTensor(),
        transforms.Normalize(
            mean=[0.48145466, 0.4578275, 0.40821073],
            std=[0.26862954, 0.26130258, 0.27577711],
        ),
    ]
)


def load_model():
    base_dir = Path(__file__).resolve().parent
    repo_dir = base_dir.parent
    config_path = os.getenv("SKIN_MODEL_CONFIG", str(repo_dir / "config" / "model-config.json"))
    config_file = Path(config_path)
    if not config_file.is_absolute():
        config_file = repo_dir / config_file

    config = {}
    if config_file.exists():
        with config_file.open("r", encoding="utf-8") as f:
            config = json.load(f)

    def conf(key: str, env: str, default: str) -> str:
        return os.getenv(env) or config.get(key, default)

    vit_checkpoint = os.getenv(
        "SKIN_MODEL_VIT_CHECKPOINT",
        conf("vit_checkpoint", "SKIN_MODEL_VIT_CHECKPOINT", str(base_dir / "weights" / "checkpoint-stage2-46.pth")),
    )
    best_model = os.getenv(
        "SKIN_MODEL_BEST_MODEL",
        conf("best_model", "SKIN_MODEL_BEST_MODEL", str(base_dir / "weights" / "best_model.pt")),
    )
    mapping_path = os.getenv(
        "SKIN_MODEL_MAPPING",
        conf("mapping", "SKIN_MODEL_MAPPING", str(base_dir / "data" / "disease_concept_mapping_final_v2.json")),
    )

    model = create_cav_model(
        panderm_checkpoint=vit_checkpoint,
        num_concepts=77,
        embed_dim=512,
        freeze_vit=True,
        unfreeze_layers=4,
        top_k=16,
        disease_mapping_path=mapping_path,
    )

    checkpoint = torch.load(best_model, map_location="cpu", weights_only=False)
    model.load_state_dict(checkpoint["model_state_dict"])
    model.eval()
    return model


def predict(model, image_path: str):
    img = Image.open(image_path).convert("RGB")
    img_tensor = TRANSFORM(img).unsqueeze(0)

    with torch.no_grad():
        outputs = model(img_tensor)

    probs = torch.softmax(outputs["disease_logits"], dim=1).squeeze()
    pred_idx = probs.argmax().item()
    confidence = probs[pred_idx].item()

    top_k_scores = outputs["top_k_scores"].squeeze()
    top_k_indices = outputs["top_k_indices"].squeeze()

    return {
        "disease": DISEASE_NAMES[pred_idx],
        "confidence": confidence,
        "top_k_indices": top_k_indices.tolist(),
        "top_k_scores": top_k_scores.tolist(),
    }


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Run skin model inference for one image")
    parser.add_argument("--image", default="test_image.jpg", help="Path to input image")
    parser.add_argument("--vit-checkpoint", help="Path to ViT checkpoint (.pth)")
    parser.add_argument("--best-model", help="Path to model checkpoint (.pt)")
    parser.add_argument("--mapping", help="Path to disease mapping json")
    parser.add_argument("--config", help="Path to config json")
    args = parser.parse_args()

    if args.config:
        os.environ["SKIN_MODEL_CONFIG"] = args.config
    if args.vit_checkpoint:
        os.environ["SKIN_MODEL_VIT_CHECKPOINT"] = args.vit_checkpoint
    if args.best_model:
        os.environ["SKIN_MODEL_BEST_MODEL"] = args.best_model
    if args.mapping:
        os.environ["SKIN_MODEL_MAPPING"] = args.mapping

    model = load_model()
    result = predict(model, args.image)
    print(result)

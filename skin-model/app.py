import os
import sys
import json
from pathlib import Path

import torch
from flask import Flask, jsonify, request
from PIL import Image
from torchvision import transforms

BASE_DIR = Path(__file__).resolve().parent
SRC_DIR = BASE_DIR / "src"
if str(SRC_DIR) not in sys.path:
    sys.path.insert(0, str(SRC_DIR))

from cav_v1.cav_model import create_cav_model

app = Flask(__name__)

DISEASE_NAMES = {
    0: "melanoma",
    1: "melanocytic_nevus",
    2: "basal_cell_carcinoma",
    3: "squamous_cell_carcinoma",
    4: "bowen_disease",
    5: "seborrheic_keratosis",
    6: "lichen_planus",
}

IMAGE_TRANSFORM = transforms.Compose(
    [
        transforms.Resize((224, 224)),
        transforms.ToTensor(),
        transforms.Normalize(
            mean=[0.48145466, 0.4578275, 0.40821073],
            std=[0.26862954, 0.26130258, 0.27577711],
        ),
    ]
)

MODEL = None
MODEL_ERROR = None
MODEL_CONFIG = {}


def _resolve_path(path_value: str) -> Path:
    path = Path(path_value)
    return path if path.is_absolute() else BASE_DIR / path


def _load_config():
    global MODEL_CONFIG
    config_path = os.getenv("SKIN_MODEL_CONFIG", str(BASE_DIR / "config" / "model-config.json"))
    config_file = Path(config_path)
    if not config_file.is_absolute():
        config_file = BASE_DIR / config_file
    if config_file.exists():
        with config_file.open("r", encoding="utf-8") as f:
            MODEL_CONFIG = json.load(f)


def _config_value(key: str, default: str) -> str:
    env_key = f"SKIN_MODEL_{key.upper()}"
    if os.getenv(env_key):
        return os.getenv(env_key)
    return MODEL_CONFIG.get(key, default)


def load_model():
    global MODEL, MODEL_ERROR
    if MODEL is not None or MODEL_ERROR is not None:
        return
    _load_config()

    vit_checkpoint = _resolve_path(
        _config_value("vit_checkpoint", "src/weights/checkpoint-stage2-46.pth")
    )
    best_model = _resolve_path(_config_value("best_model", "src/weights/best_model.pt"))
    mapping_path = _resolve_path(
        _config_value("mapping", "src/data/disease_concept_mapping_final_v2.json")
    )

    missing = [str(p) for p in [vit_checkpoint, best_model, mapping_path] if not p.exists()]
    if missing:
        MODEL_ERROR = f"Model files not found: {', '.join(missing)}"
        return

    try:
        model = create_cav_model(
            panderm_checkpoint=str(vit_checkpoint),
            num_concepts=77,
            embed_dim=512,
            freeze_vit=True,
            unfreeze_layers=4,
            top_k=16,
            disease_mapping_path=str(mapping_path),
        )
        checkpoint = torch.load(str(best_model), map_location="cpu", weights_only=False)
        model.load_state_dict(checkpoint["model_state_dict"])
        model.eval()
        MODEL = model
    except Exception as ex:
        MODEL_ERROR = f"Failed to initialize model: {ex}"


@app.get("/")
def root():
    return jsonify({"service": "skin-model", "message": "ok"})


@app.get("/health")
def health():
    load_model()
    if MODEL_ERROR is not None:
        return jsonify({"status": "error", "message": MODEL_ERROR}), 500
    return jsonify({"status": "ok"})


@app.post("/api/v1/predict")
def predict():
    load_model()
    if MODEL_ERROR is not None:
        return jsonify({"code": 500, "message": MODEL_ERROR}), 500

    image_file = request.files.get("image")
    if image_file is None:
        return jsonify({"code": 400, "message": "Missing file field: image"}), 400

    try:
        image = Image.open(image_file.stream).convert("RGB")
    except Exception:
        return jsonify({"code": 400, "message": "Invalid image file"}), 400

    with torch.no_grad():
        image_tensor = IMAGE_TRANSFORM(image).unsqueeze(0)
        outputs = MODEL(image_tensor)
        probs = torch.softmax(outputs["disease_logits"], dim=1).squeeze()

    pred_idx = int(probs.argmax().item())
    confidence = float(probs[pred_idx].item())
    top_k_scores = outputs["top_k_scores"].squeeze().tolist()
    top_k_indices = outputs["top_k_indices"].squeeze().tolist()

    return jsonify(
        {
            "code": 200,
            "message": "success",
            "data": {
                "diseaseIndex": pred_idx,
                "diseaseType": DISEASE_NAMES.get(pred_idx, "unknown"),
                "confidence": confidence,
                "topKIndices": top_k_indices,
                "topKScores": top_k_scores,
            },
        }
    )


if __name__ == "__main__":
    port = int(os.getenv("PORT", "5000"))
    app.run(host="0.0.0.0", port=port, debug=False)

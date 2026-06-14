import os
import sys
import json
from pathlib import Path

import torch
from flask import Flask, jsonify, request
from PIL import Image

BASE_DIR = Path(__file__).resolve().parent
SRC_DIR = BASE_DIR / "src"
if str(SRC_DIR) not in sys.path:
    sys.path.insert(0, str(SRC_DIR))

from xiangya_inference import Xiangya12ClassInferenceModel

app = Flask(__name__)

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

    model_type = _config_value("model_type", "xiangya_12class")
    device = _config_value("device", "cuda" if torch.cuda.is_available() else "cpu")

    if model_type != "xiangya_12class":
        MODEL_ERROR = f"Unsupported model_type: {model_type}"
        return

    model1 = _resolve_path(
        _config_value("model1", "weights/xiangya_model_primary.pt")
    )
    model2 = _resolve_path(
        _config_value("model2", "weights/xiangya_model_secondary.pt")
    )
    dataset_config = _resolve_path(
        _config_value("dataset_config", "config/xiangya_12class.json")
    )
    backbone = _resolve_path(
        _config_value("backbone", "weights/panderm_backbone.pth")
    )
    required_files = [model1, model2, dataset_config, backbone]

    missing = [
        str(p)
        for p in required_files
        if not p.exists()
    ]
    if missing:
        MODEL_ERROR = f"Model files not found: {', '.join(missing)}"
        return

    try:
        MODEL = Xiangya12ClassInferenceModel(
            model1_path=model1,
            model2_path=model2,
            config_path=dataset_config,
            backbone_path=backbone,
            device=device,
        )
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
    return jsonify(
        {
            "status": "ok",
            "model": MODEL.model_name,
            "device": str(MODEL.device),
            "conceptCount": len(MODEL.concepts),
        }
    )


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

    try:
        top_k = int(request.form.get("topK", 16))
        heatmap_top_k = int(request.form.get("heatmapTopK", 3))
        heatmap_alpha = float(request.form.get("heatmapAlpha", 0.5))
        heatmap_max_size = int(request.form.get("heatmapMaxSize", 768))
        if not 1 <= top_k <= 80:
            raise ValueError("topK must be between 1 and 80")
        if not 1 <= heatmap_top_k <= top_k:
            raise ValueError("heatmapTopK must be between 1 and topK")
        if not 0 <= heatmap_alpha <= 1:
            raise ValueError("heatmapAlpha must be between 0 and 1")
        if not 256 <= heatmap_max_size <= 2048:
            raise ValueError("heatmapMaxSize must be between 256 and 2048")
    except ValueError as ex:
        return jsonify({"code": 400, "message": str(ex)}), 400

    try:
        data = MODEL.predict(
            image=image,
            top_k=top_k,
            heatmap_top_k=heatmap_top_k,
            heatmap_alpha=heatmap_alpha,
            heatmap_max_size=heatmap_max_size,
        )
        return jsonify({"code": 200, "message": "success", "data": data})
    except Exception as ex:
        app.logger.exception("Prediction failed")
        return jsonify({"code": 500, "message": f"Prediction failed: {ex}"}), 500


if __name__ == "__main__":
    port = int(os.getenv("PORT", "5000"))
    app.run(host="0.0.0.0", port=port, debug=False)

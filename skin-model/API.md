# Skin Model API

## Start

```bash
pip install -r requirements.txt
python app.py
```

The service reads `config/model-config.json` by default. Set
`SKIN_MODEL_CONFIG` to use another configuration file.

## Predict

`POST /api/v1/predict` accepts `multipart/form-data`:

- `image`: required JPG or PNG image.
- `topK`: number of returned concepts, default `16`.
- `heatmapTopK`: concepts fused into the heatmap, default `3`.
- `heatmapAlpha`: overlay opacity from `0` to `1`, default `0.5`.
- `heatmapMaxSize`: maximum heatmap edge in pixels, default `768`.

Example:

```bash
curl -X POST http://127.0.0.1:5000/api/v1/predict \
  -F "image=@test.jpg" \
  -F "topK=10" \
  -F "heatmapTopK=3"
```

The response keeps `diseaseIndex`, `diseaseType`, `confidence`,
`topKIndices`, and `topKScores` for compatibility. It also returns complete
disease probabilities, concept metadata and contributions, and a PNG heatmap
encoded as Base64.

The default runtime uses the two Xiangya 12-class checkpoints with fusion
weights `0.3` and `0.7`. The heatmap follows the processing pipeline in
`src/tools/visualize_xiangya.py`: positive patch evidence, top-30%
patch filtering, disease-contribution ranking, weighted Top-K fusion, lesion
evidence fusion, bicubic resizing, bilateral filtering, min-max normalization,
Inferno coloring, alpha blending, and white activation contours.
`heatmap.concepts` contains the exact concepts used to generate the heatmap;
these can differ from the concepts ranked only by their activation score.

The experimental LAB lesion mask is intentionally disabled in the Flask
pipeline because the supplied reference visualization was generated without
it; enabling it changes the highlighted regions substantially.

`GET /health` loads the model and reports its device and concept count.

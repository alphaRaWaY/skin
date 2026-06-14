# Skin Model Service

Flask inference service for the Xiangya 12-class dual-model skin diagnosis
pipeline.

## Structure

```text
skin-model/
├── config/          # Runtime and disease/concept configuration
├── data/examples/   # Local inference samples
├── src/
│   ├── models/      # PanDerm backbone implementation
│   ├── tools/       # Standalone visualization tools
│   ├── vilc_bm/     # ViLCBM model implementation
│   └── xiangya_inference.py
├── weights/         # Local model checkpoints, ignored by Git
├── app.py
└── requirements.txt
```

## Run

```bash
pip install -r requirements.txt
python app.py
```

The service loads `config/model-config.json`. Copy
`config/model-config.example.json` when preparing a new environment.

## Verify

```bash
python src/tools/visualize_xiangya.py \
  --image data/examples/example2.jpg \
  --true_label 11 \
  --device cpu
```

Model weights are intentionally excluded from Git. Use
`scripts/sync-skin-model.ps1 -IncludeWeights` when deploying them directly.

# Model acquisition

The app bundles **BiRefNet Lite 512**, exported to ONNX FP16 by Studio Ludens from the upstream `ZhengPeng7/BiRefNet_lite` checkpoint.

The 98,484,532-byte binary is intentionally excluded from Git history. Before building, run:

```powershell
.\scripts\acquire-model.ps1
```

The script downloads a pinned Hugging Face revision and verifies SHA-256:

`EFF9216BB2F9D3F023D9C2B7196845A7485739AB1F231593633E4D2344FFC516`

Input is RGB `1×3×512×512` with ImageNet normalization. Output is a `512×512` logits matte; the app applies sigmoid and maps the alpha matte back onto the safely decoded source image.

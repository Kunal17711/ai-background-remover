param(
    [string]$Destination = (Join-Path $PSScriptRoot '..\app\src\main\assets\models\birefnet-lite-512-fp16.onnx')
)

$ErrorActionPreference = 'Stop'
$modelUrl = 'https://huggingface.co/studioludens/birefnet-lite-512/resolve/4a3c40c36c94093cc1e724d9ea428b8fa4b57dc7/onnx/model_fp16.onnx'
$expectedSha256 = 'EFF9216BB2F9D3F023D9C2B7196845A7485739AB1F231593633E4D2344FFC516'
$resolvedDestination = [System.IO.Path]::GetFullPath($Destination)
$destinationDirectory = [System.IO.Path]::GetDirectoryName($resolvedDestination)

[System.IO.Directory]::CreateDirectory($destinationDirectory) | Out-Null

if (Test-Path -LiteralPath $resolvedDestination) {
    $existingHash = (Get-FileHash -LiteralPath $resolvedDestination -Algorithm SHA256).Hash
    if ($existingHash -eq $expectedSha256) {
        Write-Host "Verified existing BiRefNet Lite model: $resolvedDestination"
        exit 0
    }
}

$temporaryFile = "$resolvedDestination.download"
try {
    Invoke-WebRequest -Uri $modelUrl -OutFile $temporaryFile
    $actualHash = (Get-FileHash -LiteralPath $temporaryFile -Algorithm SHA256).Hash
    if ($actualHash -ne $expectedSha256) {
        throw "Model checksum mismatch. Expected $expectedSha256 but received $actualHash."
    }
    Move-Item -LiteralPath $temporaryFile -Destination $resolvedDestination -Force
    Write-Host "Downloaded and verified BiRefNet Lite model: $resolvedDestination"
} finally {
    if (Test-Path -LiteralPath $temporaryFile) {
        Remove-Item -LiteralPath $temporaryFile -Force
    }
}

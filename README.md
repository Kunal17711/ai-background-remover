# AI Background Remover

A private, offline Android app that removes image backgrounds on-device. Select a photo, let BiRefNet create an alpha matte, preview the transparent result, then save a PNG or share it.

[Download the latest APK](https://github.com/Kunal17711/ai-background-remover/releases/latest/download/ai-background-remover.apk)

## Preview

| Original | Background removed |
| --- | --- |
| ![Original cat photo](docs/screenshots/demo-original.webp) | ![Cat cutout produced by the bundled model](docs/screenshots/demo-cutout.webp) |

The cutout above was produced with the same pinned ONNX graph bundled by release builds. Source photo: RobotBlanket, [CC0 via Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Cat_face_in_sunlight.jpg).

## Features

- Fully on-device processing; the app declares no Internet permission
- Android Photo Picker, with no broad photo-library permission
- Transparent PNG export through MediaStore
- Secure sharing through a temporary cache file and `FileProvider`
- Before/after comparison, dark theme, landscape and tablet layouts
- Bounded image decoding, EXIF correction, cancellation and stale-result protection
- Android 8.0+ (`minSdk 26`)

## Privacy

Photos never leave the device. There are no accounts, analytics, ads, tracking SDKs, cookies, backend services, or network calls. Read [PRIVACY.md](PRIVACY.md) for the complete policy.

## Technology

- Kotlin, Jetpack Compose and Material 3
- Manrope typography, bundled under the SIL Open Font License 1.1
- ONNX Runtime for Android
- BiRefNet Lite 512, FP16 ONNX
- MVVM-style state flow with constructor-injected dependencies

The model accepts a normalized `1 x 3 x 512 x 512` RGB tensor and emits a logits matte. The app applies sigmoid, maps the alpha matte to the safely decoded source resolution, and preserves any existing source alpha. Model acquisition and provenance are documented in [MODEL.md](MODEL.md).

## Build

Requirements: JDK 17 and Android SDK 36.

```powershell
.\scripts\acquire-model.ps1
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

Release builds are unsigned unless all four signing values are supplied as environment variables or Gradle properties:

```text
RELEASE_STORE_FILE
RELEASE_STORE_PASSWORD
RELEASE_KEY_ALIAS
RELEASE_KEY_PASSWORD
```

Then run:

```powershell
.\gradlew.bat assembleRelease bundleRelease
```

The tag-driven GitHub Actions release workflow uses equivalent repository secrets, verifies the APK signature, and publishes `ai-background-remover.apk` plus the Android App Bundle.

## Testing

Unit tests cover decode planning, file naming, matte conversion, and state transitions including stale-result protection. Compose instrumentation tests cover the initial picker action and result actions; run them on an emulator or device with `connectedDebugAndroidTest`.

## Contributing and security

See [CONTRIBUTING.md](CONTRIBUTING.md) before proposing a change. Please report security or privacy issues using the private process in [SECURITY.md](SECURITY.md).

Created by [Kunal Builds](https://instagram.com/bykunalbuilds).

## License

Application source is available under the [MIT License](LICENSE). The model, runtime, and Android libraries retain their respective licenses; see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

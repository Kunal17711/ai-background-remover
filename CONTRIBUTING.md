# Contributing

Thanks for helping improve AI Background Remover.

1. Open an issue describing the problem or proposed change.
2. Keep the app offline-first: do not add `INTERNET`, analytics, ads, tracking, accounts, or remote processing.
3. Keep large model binaries out of Git. Use the pinned acquisition script and document any proposed model change with its source, exact license, checksum, size, quality evidence, and device impact.
4. Add or update tests for behavior changes.
5. Run the required checks before opening a pull request:

```powershell
.\scripts\acquire-model.ps1
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
```

Use focused commits and explain user-visible changes, privacy impact, and manual validation in the pull request.

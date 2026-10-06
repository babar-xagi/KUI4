# 📦 APK Packaging

The Kotlin packaging pipeline:

1. Write `classes.dex`.
2. Generate a binary `AndroidManifest.xml`.
3. Copy application assets into a ZIP archive.
4. Align uncompressed entries to four-byte boundaries.
5. Add an APK Signature Scheme v2 signing block using RSA-2048 and SHA-256.
6. Verify the signature and archive integrity, then write the APK.

The final path is `build/outputs/apk/debug/app-debug.apk` inside the project. Keys are debug signing keys, not a production release-signing workflow.

There is no complete `resources.arsc` resource compiler, v3 signing implementation, or native-library page-alignment pipeline. Asset packaging does not imply an implemented image or font renderer.

[Compiler and packaging details](developer/toolchain-and-compiler.md) · [Project guide](user/project-guide.md)
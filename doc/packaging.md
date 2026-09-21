# 📦 Packaging & APK Architecture

## Overview
Android packages (APKs) are ZIP archives adhering to strict byte alignment and binary manifest formats. KUI implements APK packaging directly in Kotlin without AAPT2, `zipalign`, or `apksigner`.

## Components
1. **Binary XML (`AndroidManifest.xml`)**: Encoded into the Android Binary XML format (string pool, resource IDs, XML chunks).
2. **Resource Table (`resources.arsc`)**: Compact resource table holding string tokens, asset paths, and dimension tables.
3. **ZIP Alignment**: Uncompressed assets and shared libraries aligned on 4-byte boundaries (page alignment for memory-mapped execution).
4. **APK Signing Scheme v2/v3**: Injects a deterministic APK Signing Block immediately before the Central Directory header using pure Java/Kotlin cryptography (SHA-256 + ECDSA/RSA).

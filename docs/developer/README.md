# KUI Developer & Contributor Guide

Welcome to the **KUI (Kotlin UI) Developer Guide**. This documentation is written for core maintainers, contributors, and systems engineers who want to understand the internal mechanics of KUI, modify the platform, or contribute new features.

---

## 🌟 Philosophy & Architectural North Star

Traditional Android development suffers from heavy toolchain overhead:
- Gradle build scripts with thousands of lines of Groovy/Kotlin DSL
- Android Gradle Plugin (AGP) layers
- AAPT2 resource compilation and table packing
- D8/R8 dexing and desugaring overhead
- `apksigner` and `zipalign` external process spawning
- Massive GB-sized installations of Android Studio and Android SDK build-tools

**KUI takes a radically different approach: 100% Pure Kotlin Self-Hosting.**
1. **Zero Gradle:** No Gradle daemon, no AGP, no Groovy/Kotlin DSL configuration overhead.
2. **Zero AAPT2:** Direct binary Android XML (`AndroidManifest.xml`) emission in pure Kotlin (`AxmlWriter`).
3. **Zero D8:** JVM bytecode to Dalvik bytecode translation in pure Kotlin (`ClassToDexCompiler`, `DexModel`).
4. **Zero apksigner:** Cryptographic APK Signature Scheme v2 implemented in pure Kotlin using standard JDK `java.security` primitives (`ApkV2Signer`).
5. **Zero zipalign:** Pure Kotlin 4-byte boundary aligned ZIP/APK generation (`ApkWriter`).
6. **Zero Android Studio dependency:** Entire end-to-end build, package, sign, install, and run happens in milliseconds using pure Kotlin on top of JDK 21.

---

## 🧭 Developer Documentation Roadmap

To navigate the developer documentation, explore the following dedicated guides:

| Guide | Description |
| :--- | :--- |
| **[Architecture & Pipeline](architecture.md)** | Detailed architectural diagrams, pipeline execution stages, data flow from source code to on-device rendering. |
| **[Directory Structure & File Reference](directory-structure.md)** | File-by-file breakdown of the entire repository, describing every package and source file's purpose. |
| **[Toolchain & Compiler Internals](toolchain-and-compiler.md)** | Deep dive into `ClassFileReader`, `ClassToDexCompiler`, `DexModel`, instruction fixups, MUTF-8 encoding, AXML generation, APK alignment, APK v2 signing, and ADB integration. |
| **[UI4 Engine Architecture](ui4-engine.md)** | In-depth guide to the 2-pass layout engine, `RecordingCanvas`, reactive state, dirty tracking, input dispatch, animation loop, and accessibility. |
| **[Contributing & Testing Guide](contributing.md)** | Environment setup, bootstrap script (`kui.ps1` / `kui.bat`), running milestone test suites, adding new unit tests, and verifying on physical hardware. |

---

## ⚡ Quick Architecture Overview

```
                          ┌───────────────────────┐
                          │   kui.toml / Config   │
                          └──────────┬────────────┘
                                     │
┌────────────────────────────────────┼──────────────────────────────────┐
│ KUI Toolchain (platform/kui)       │                                  │
│                                    ▼                                  │
│ ┌────────────────┐        ┌──────────────────┐       ┌──────────────┐ │
│ │ Kotlin Source  │ ─────> │ Kotlinc Driver   │ ────> │ JVM .class   │ │
│ │ (src/*.kt)     │        │ (Direct Compile) │       │ Files        │ │
│ └────────────────┘        └──────────────────┘       └──────┬───────┘ │
│                                                             │         │
│                                                             ▼         │
│                                                    ┌────────────────┐ │
│                                                    │ ClassFileRead  │ │
│                                                    └────────┬───────┘ │
│                                                             │         │
│                                                             ▼         │
│ ┌────────────────┐        ┌──────────────────┐     ┌────────────────┐ │
│ │ ManifestGen    │        │ Asset Packager   │     │ ClassToDex     │ │
│ │ (Binary AXML)  │        │ (assets/)        │     │ (Pure Dalvik)  │ │
│ └───────┬────────┘        └────────┬─────────┘     └────────┬───────┘ │
│         │                          │                        │         │
│         └───────────────────┬──────┴────────────────────────┘         │
│                             ▼                                         │
│                   ┌──────────────────┐                                │
│                   │ ApkWriter (ZIP)  │ (4-byte zipalign)              │
│                   └─────────┬────────┘                                │
│                             │                                         │
│                             ▼                                         │
│                   ┌──────────────────┐                                │
│                   │ ApkV2Signer      │ (APK Signature Scheme v2, RSA) │
│                   └─────────┬────────┘                                │
│                             │                                         │
│                             ▼                                         │
│                   ┌──────────────────┐                                │
│                   │ DeviceManager    │ (ADB install & am start)       │
│                   └─────────┬────────┘                                │
└─────────────────────────────┼─────────────────────────────────────────┘
                              │
                              ▼
                   ┌───────────────────────┐
                   │ Physical Android Phone│
                   │ / Emulator (ART)      │
                   └───────────────────────┘
```

---

## 🧪 Testing the Platform

All platform features are accompanied by self-contained regression test suites:
- Milestone A: Repository bootstrap, configuration, directories
- Milestone B: Kotlinc integration, class parsing, AST verification
- Milestone C: Incremental build, caching, hashing, benchmark profiling
- Milestone D: UI4 core layout, trees, dirty tracking, rendering
- Milestone E: UI4 state bindings, gestures, text measurement, animations
- Milestone F: Virtual display host, focus management, input dispatch, semantics
- Milestone G: Pure Kotlin DEX compiler, AXML generator, APK v2 signer, ADB deployment

To run all Milestone G tests:
```powershell
.\scripts\test_milestone_g.bat
```
Output: `Milestone G Test Results: 78 PASSED, 0 FAILED`.

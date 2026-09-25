# 🛠️ KUI Developer & Contributor Guide

Welcome to the **KUI (Kotlin UI) Developer Guide**! 🚀  
This documentation is for core maintainers, systems engineers, and contributors who want to understand the internal architecture of KUI, work on its native Rust toolchain, or extend the UI4 declarative framework.

---

## 🌟 Architectural Philosophy

Traditional mobile development relies on an excessively complex and heavy toolchain:
* Gradle daemon and Android Gradle Plugin (AGP) with thousands of lines of DSL overhead.
* AAPT2 resource compiler and table packaging (C++ binaries).
* D8 / R8 JVM bytecode to Dalvik bytecode translators and desugarers.
* External process spawning of `apksigner` and `zipalign`.
* 15 GB+ downloads of Android Studio and SDK platforms.

### The KUI Solution: Dual-Layer Self-Hosting Architecture

KUI separates responsibilities into two high-performance, purpose-built layers:

```
┌────────────────────────────────────────────────────────────────────────┐
│               1. Declarative Kotlin UI Layer (platform/ui4)            │
│  Declarative DSL • 2-Pass Layout • Reactive State • Canvas Rendering   │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │ JVM .class Bytecode
┌──────────────────────────────────┴─────────────────────────────────────┐
│                 2. Native Rust Systems Layer (crates/)                 │
│                                                                        │
│  ┌───────────────────────┐  ┌──────────────────┐  ┌─────────────────┐  │
│  │ kui-cli               │  │ kui-dex          │  │ kui-packager    │  │
│  │ Sub-5ms CLI Dispatch  │  │ Pure Dalvik DEX  │  │ AXML, 4B Align, │  │
│  │ Diagnostics & ADB     │  │ Opcode Compiler  │  │ APK v2 Signer   │  │
│  └───────────────────────┘  └──────────────────┘  └─────────────────┘  │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │ 4-Byte Aligned, v2-Signed APK
                                   ▼
                       Android Physical Device / ART
```

1. **Native Rust Systems Layer (`crates/`)**:
   * **`crates/kui-cli`**: Sub-5ms native binary bootstrapper (`kui.exe`) handling command dispatch, diagnostics (`kui doctor`), and ADB device management (`kui devices`, `kui run`).
   * **`crates/kui-dex`**: Pure Rust Dalvik Executable compiler. Directly parses JVM `.class` bytecode, allocates registers, maps instructions to Dalvik opcodes, constructs MUTF-8 string pools, and writes binary `classes.dex` with Adler-32 / SHA-1 checksums. Validated with Google's official `dexdump.exe`.
   * **`crates/kui-packager`**: Pure Rust APK builder. Generates binary `AndroidManifest.xml` (`AxmlWriter`), creates 4-byte memory-aligned ZIP archives (`ApkWriter`), and computes APK Signature Scheme v2 RSA-2048 tree hashes (`ApkV2Signer`).
2. **Declarative Kotlin Layer (`platform/ui4/`)**:
   * Pure declarative Kotlin UI engine (`UI4`).
   * 2-pass layout system (`measure` & `layout`), reactive state primitives (`mutableStateOf`), dirty tracking, touch dispatch, and canvas rendering.

---

## 🧭 Developer Documentation Roadmap

Explore the dedicated technical guides below:

| 📘 Guide | 🔍 Topic & Description |
| :--- | :--- |
| **[🏗️ Architecture & Pipeline](architecture.md)** | End-to-end architectural diagram and data flow from Kotlin source to on-device rendering. |
| **[🗂️ Directory Structure Reference](directory-structure.md)** | Complete file-by-file breakdown of the entire repository across Rust crates and Kotlin packages. |
| **[⚙️ kui-dex Internals](kui-dex-internals.md)** | Technical deep-dive into the pure Rust DEX compiler: MUTF-8 encoding, ULEB128, JVM class parsing, opcode translation, and dexdump verification. |
| **[🎨 UI4 Engine Architecture](ui4-engine.md)** | Layout contracts, `RecordingCanvas`, dirty propagation, hit testing, and gestures. |
| **[🤝 Contributing & Testing](contributing.md)** | Developer environment setup, running tests (`cargo test --workspace`), and building MSI installers. |

---

## ⚡ Quick Build & Test Instructions

### Prerequisites:
* Rust toolchain (stable 1.80+): `rustup update stable`
* Java Development Kit (JDK 21+)
* Standalone Kotlin compiler (`kotlinc` 2.0+)
* WiX Toolset v5 (for MSI packaging): `dotnet tool install --global wix --version 5.0.2`

### Building from Source:
```powershell
# Build all Rust crates in release mode
cargo build --release --workspace
```

### Running All Unit and Integration Tests:
```powershell
# Runs 33+ tests across kui-cli, kui-packager, and kui-dex
cargo test --workspace
```

### Building the Windows MSI Installer:
```powershell
pwsh -File scripts/build_msi.ps1 -Version 0.3.0 -MsiName 0.03rs_kui
```
The output installer will be generated at `dist/0.03rs_kui.msi`.

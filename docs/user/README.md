# 📖 KUI User Guide & Documentation

Welcome to the **KUI (Kotlin UI) User Guide**! 🚀  
KUI is a next-generation, high-performance application development platform that lets you build native Android mobile applications with pure declarative Kotlin — completely eliminating the overhead of Gradle, Android Gradle Plugin (AGP), AAPT2, D8, and heavy Android Studio installations.

---

## ⚡ Why Choose KUI?

| 🛠️ Capability | 🐢 Traditional Android Toolchain | 🚀 KUI Platform & Native Toolchain |
| :--- | :--- | :--- |
| **Build System** | Gradle Daemon + AGP (thousands of lines of DSL) | **Zero Gradle** — Pure Rust Native CLI (`kui-cli`) |
| **Binary XML Emission** | AAPT2 (C++ binary) | **Pure Rust AxmlWriter** (`kui-packager`) |
| **DEX Bytecode Generation** | D8 / R8 (Heavy JVM tool) | **Pure Rust ClassToDexCompiler** (`kui-dex`) |
| **APK Memory Alignment** | `zipalign` (C++ command-line tool) | **Pure Rust 4-byte memory-aligned ApkWriter** |
| **APK Signing** | `apksigner` / jarsigner | **Pure Rust ApkV2Signer** (APK Signature Scheme v2, RSA) |
| **UI Framework** | Android View / Jetpack Compose | **UI4 Engine** (declarative DSL, 2-pass layout, reactive state) |
| **Disk Footprint** | 15 GB – 20 GB download | **Lightweight** — Only standard JDK 21 and standalone `kotlinc` |
| **CLI Startup Latency** | 3s – 10s JVM initialization | **Sub-5ms native binary speed** |
| **Incremental Build** | 15s – 45s | **Sub-second (<500ms)** |

---

## 📚 User Documentation Table of Contents

Explore the guides below to master KUI application development:

1. **[🚀 Getting Started & Installation](getting-started.md)**  
   System prerequisites, one-click Windows MSI installation (`0.03rs_kui.msi`), manual setup, and running `kui doctor`.

2. **[🧭 CLI Command Reference](cli-reference.md)**  
   Complete guide to every native CLI command: `kui new`, `kui build`, `kui run`, `kui install`, `kui launch`, `kui devices`, `kui doctor`, `kui clean`, `kui test`, and `kui info`.

3. **[🗂️ Project Anatomy & Guide](project-guide.md)**  
   Scaffolding projects with `kui new`, understanding `kui.toml`, `src/main.kt`, static assets (`assets/fonts`, `assets/images`), and build artifacts.

4. **[🎨 UI4 Components & Styling Reference](ui-components-and-styling.md)**  
   Complete guide to declarative UI building: layouts (`column`, `row`, `box`, `center`, `stack`), interactive widgets (`text`, `button`, `textField`), reactive state (`mutableStateOf`), and styling.

5. **[🍳 Cookbook & Examples](cookbook-and-examples.md)**  
   Production-ready examples: Counter App, Interactive Todo List, Form Validation, Custom Card Dashboard, and physical Android phone deployment.

6. **[🩺 Troubleshooting & FAQ](troubleshooting.md)**  
   Resolving common issues, physical device USB debugging, ADB authorization, and upgrading from older versions.

---

## ⚡ 60-Second Quick Start

Get your first application running on a connected phone or emulator in under a minute:

```powershell
# 1. Verify your environment
kui doctor

# 2. Check for connected Android devices or emulators
kui devices

# 3. Create a new project
kui new todo
cd todo

# 4. Build, package, sign, install, and run on your device
kui run
```

---

## 📥 Direct Installer Downloads

| Release Tag | Installer | Version | Highlights | Download Link |
| :--- | :--- | :--- | :--- | :--- |
| **`v0.03rs_kui`** | `0.03rs_kui.msi` | **0.3.0 (Latest)** | Native `kui-dex` compiler, opcode translator, Google `dexdump` verified | 📥 [**Download 0.03rs_kui.msi (6.65 MB)**](../../releases/v0.03rs_kui/0.03rs_kui.msi) |
| **`v0.02rs_kui`** | `0.02rs_kui.msi` | **0.2.2** | Native `kui-packager`, pure Rust AXML, 4-byte zipalign, APK v2 signing | 📥 [**Download 0.02rs_kui.msi (6.57 MB)**](../../releases/v0.02rs_kui/0.02rs_kui.msi) |
| **`v0.01rs_kui`** | `0.01rs_kui.msi` | **0.2.1** | Native CLI bootstrapper (`kui-cli`), project generator, doctor diagnostics | 📥 [**Download 0.01rs_kui.msi (6.55 MB)**](../../releases/v0.01rs_kui/0.01rs_kui.msi) |

See full SHA-256 cryptographic checksums in [**RELEASES.md**](../../RELEASES.md).

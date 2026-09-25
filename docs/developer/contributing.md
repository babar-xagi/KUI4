# 🤝 KUI Contributor & Development Guide

Welcome to the **KUI Platform** developer guide! We are building a lightning-fast, zero-Gradle, zero-Android-Studio mobile toolchain and declarative UI framework for Android powered by native Rust systems programming and modern Kotlin.

This document covers everything you need to set up your development environment, build all workspace crates, run automated test suites, build Windows MSI installers, and contribute to the repository.

---

## 📋 Table of Contents

1. [🛠️ Prerequisites & Toolchain](#️-prerequisites--toolchain)
2. [🏗️ Project Architecture Overview](#️-project-architecture-overview)
3. [🔨 Building the Workspace](#-building-the-workspace)
4. [🧪 Running Test Suites](#-running-test-suites)
5. [📦 Building Windows MSI Installers](#-building-windows-msi-installers)
6. [📱 Device Testing & Hardware Verification](#-device-testing--hardware-verification)
7. [📐 Code Standards & Conventions](#-code-standards--conventions)
8. [🚀 Pull Request & Contribution Workflow](#-pull-request--contribution-workflow)

---

## 🛠️ Prerequisites & Toolchain

To contribute to KUI's native systems layer and platform engine, ensure you have the following installed:

| Tool | Minimum Version | Purpose | Installation / Check |
| :--- | :--- | :--- | :--- |
| **Rust & Cargo** | 1.80+ (Stable) | Compiles native crates (`kui-cli`, `kui-packager`, `kui-dex`) | `rustc --version` / [rustup.rs](https://rustup.rs) |
| **Java JDK** | 21+ (Temurin / OpenJDK) | Runs Kotlin compiler & runtime checks | `java -version` |
| **Kotlin Compiler** | 2.0+ (`kotlinc`) | Compiles declarative UI and user app code | `kotlinc -version` |
| **Android ADB** | 1.0.41+ | Deploys & launches APKs on hardware/emulators | `adb version` |
| **WiX Toolset** | v5.0+ (`wix.exe`) | Builds Windows `.msi` installers | `dotnet tool install --global wix` |
| **PowerShell** | 7.0+ (`pwsh`) | Automation scripts for testing and packaging | `pwsh --version` |

> [!NOTE]
> **Zero Heavy Android Tooling Required:** You do **not** need Android Studio, Gradle, Gradle wrappers, AAPT2, D8, or Android SDK build-tools to build KUI or develop applications with it!

---

## 🏗️ Project Architecture Overview

KUI utilizes a high-performance **Dual-Layer Architecture**:

```
┌─────────────────────────────────────────────────────────────┐
│ 🦀 Native Rust Systems Layer (crates/)                      │
│   ├── kui-cli: High-speed developer CLI & process driver    │
│   ├── kui-dex: Pure Rust Dalvik Executable (DEX) compiler   │
│   └── kui-packager: Memory-aligned ZIP + APK v2 signer      │
├─────────────────────────────────────────────────────────────┤
│ 💎 Declarative Kotlin UI Layer (platform/)                  │
│   ├── platform/ui4: High-performance canvas layout engine   │
│   └── platform/kui: Android platform bridge & runtime       │
└─────────────────────────────────────────────────────────────┘
```

1. **Native Systems Layer (`crates/`):** Written in pure, safe Rust. Performs instant CLI execution, zero-overhead process orchestration, direct JVM-to-DEX bytecode translation, 4-byte memory alignment (zipalign), and cryptographic APK Signature Scheme v2 generation.
2. **Declarative UI Layer (`platform/`):** Written in Kotlin. Renders lightweight, 60fps+ declarative UI components directly to Android surfaces without AndroidX or Jetpack Compose overhead.

---

## 🔨 Building the Workspace

### 1. Compile Native Rust Crates
To compile all crates (`kui-cli`, `kui-packager`, `kui-dex`) in debug mode:
```powershell
cargo build --workspace
```

For release builds with link-time optimization (LTO) and binary stripping:
```powershell
cargo build --workspace --release
```
The optimized native binary will be generated at `target/release/kui.exe`.

### 2. Verify Platform Kotlin Code
You can verify the Kotlin platform code and UI4 engine using `kotlinc`:
```powershell
kotlinc -Werror platform/ui4/src/main/kotlin/ui4/**/*.kt -d .kui/build/ui4.jar
```

---

## 🧪 Running Test Suites

### 1. Native Rust Tests (Unit & Integration)
Run all 33+ native Rust tests across `kui-cli`, `kui-packager`, and `kui-dex`:
```powershell
cargo test --workspace
```

Run tests with verbose output:
```powershell
cargo test --workspace -- --nocapture
```

Run tests for a specific crate:
```powershell
cargo test -p kui-dex
cargo test -p kui-packager
cargo test -p kui-cli
```

### 2. Kotlin Platform Milestone Batteries
The repository includes automated test batteries validating platform components:

```powershell
# Milestone D: UI4 Core Layout & Node Tree
.\scripts\test_milestone_d.bat

# Milestone E: Reactive State Bindings & Gestures
.\scripts\test_milestone_e.bat

# Milestone F: Virtual Host Surface & Focus Tree
.\scripts\test_milestone_f.bat

# Milestone G: Pure Android Toolchain (78 tests)
.\scripts\test_milestone_g.bat
```

---

## 📦 Building Windows MSI Installers

KUI provides a complete WiX Toolset v5 automation script to produce production-ready, signed Windows Installer `.msi` packages.

To build an installer:
```powershell
pwsh -File scripts/build_msi.ps1 -Version 0.3.0 -MsiName 0.03rs_kui
```

### What `build_msi.ps1` does:
1. Compiles optimized `kui.exe` using `cargo build --release`.
2. Packages platform runtime libraries (`platform/kui`, `platform/ui4`).
3. Generates WiX authoring files (`wix/kui.wxs`) configuring `INSTALLDIR` (`C:\Program Files\KUI`) and adding KUI to the system `PATH`.
4. Compiles and links `0.03rs_kui.msi`.
5. Computes and generates the SHA-256 checksum file.
6. Archives artifacts in `releases/v0.03rs_kui/` and `dist/`.

---

## 📱 Device Testing & Hardware Verification

When testing APK generation and execution on physical hardware:

1. **Verify ADB Connection:**
   ```powershell
   kui devices
   ```
2. **Generate and Run a Test Application:**
   ```powershell
   cd Desktop
   kui new mytestapp
   cd mytestapp
   kui run
   ```
3. **Inspect Real-time Android Logcat:**
   ```powershell
   adb logcat -s AndroidRuntime ActivityTaskManager kui com.example.mytestapp
   ```
4. **Verify Process State:**
   ```powershell
   adb shell dumpsys activity top | Select-String "ACTIVITY"
   ```

---

## 📐 Code Standards & Conventions

### 🦀 Rust Guidelines (`crates/`)
- **Zero Heavy C Dependencies:** Keep crates pure Rust to ensure cross-compilation simplicity and security.
- **Error Handling:** Use `std::io::Result` or dedicated enum error types. Never use `unwrap()` or `expect()` in library code; return errors gracefully.
- **Deterministic Output:** DEX emission and APK signing must produce bit-for-bit deterministic binaries when given identical inputs.
- **Formatting:** Run `cargo fmt --check` before submitting pull requests.
- **Linting:** Run `cargo clippy --workspace -- -D warnings` to maintain zero compiler warnings.

### 💎 Kotlin Guidelines (`platform/`)
- **No Third-Party Dependencies:** Rely solely on the standard Kotlin/Java runtime libraries (`java.nio`, `java.util`, `java.security`).
- **Memory Efficiency:** Avoid excessive allocations in inner layout and rendering loops.
- **Naming Conventions:** Class names in `PascalCase`, functions and variables in `camelCase`, constants in `UPPER_SNAKE_CASE`.

---

## 🚀 Pull Request & Contribution Workflow

1. **Fork & Branch:** Create a feature branch from `main`:
   ```bash
   git checkout -b feat/your-feature-name
   ```
2. **Implement & Test:** Write your code and add corresponding unit/integration tests:
   ```bash
   cargo test --workspace
   ```
3. **Update Documentation:** If modifying CLI flags, configuration options, or internal architectures, update the corresponding markdown documents in `docs/user/` or `docs/developer/`.
4. **Update Changelog:** Add an entry under `## [Unreleased]` in [`CHANGELOG.md`](../../CHANGELOG.md).
5. **Commit:** Follow Conventional Commits:
   - `feat(dex): support invoke-interface instructions`
   - `fix(packager): ensure 4-byte padding on APK entries`
   - `docs(user): add troubleshooting for USB debugging`
6. **Open PR:** Submit your pull request to `main` with a clear explanation of changes, test results, and device verification screenshots if applicable.

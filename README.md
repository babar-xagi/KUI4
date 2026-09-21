<div align="center">

# 🟣 KUI (Kotlin UI)
### The Pure Kotlin Application Platform & Toolchain

**Build native Android applications with declarative Kotlin.**  
**Zero Gradle. Zero AGP. Zero AAPT2. Zero D8. Zero Android Studio.**

[![Kotlin Version](https://img.shields.io/badge/Kotlin-2.0%2B-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![JDK Requirement](https://img.shields.io/badge/JDK-21%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://adoptium.net)
[![Platform](https://img.shields.io/badge/Platform-Android%20(API%2024%2B)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue?style=for-the-badge)](LICENSE)
[![Tests Status](https://img.shields.io/badge/Tests-78%2F78%20Passing-brightgreen?style=for-the-badge)]()
[![Build Time](https://img.shields.io/badge/Incremental%20Build-%3C500ms-success?style=for-the-badge)]()

[**Download Release**](https://github.com/babar-xagi/KUI4/releases) • [**Getting Started**](#-quick-installation--setup) • [**Documentation**](docs/user/README.md) • [**Developer Guide**](docs/developer/README.md)

</div>

---

## ⚡ Why KUI?

Traditional Android development requires **15GB+** of downloads (Android Studio, SDK platforms, build-tools, NDK), thousands of lines of fragile Gradle DSL configuration, and multi-minute cold build times.

**KUI replaces the entire build toolchain with pure Kotlin running on standard JDK 21.**

| Capability | Traditional Android Toolchain | KUI Pure Kotlin Platform |
| :--- | :--- | :--- |
| **Build Tooling** | Gradle Daemon + Android Gradle Plugin (AGP) | **Zero Gradle** — Built-in pure Kotlin orchestrator |
| **XML Compilation** | AAPT2 (C++ binary) | **Pure Kotlin AxmlWriter** (`RES_XML_TYPE`, `RES_RESOURCE_MAP`) |
| **DEX Compilation** | D8 / R8 (Heavy JVM tool) | **Pure Kotlin ClassToDexCompiler** (Direct Dalvik bytecode) |
| **APK Alignment** | `zipalign` (C++ command-line tool) | **Pure Kotlin 4-byte memory-aligned ApkWriter** |
| **APK Signing** | `apksigner` / jarsigner | **Pure Kotlin ApkV2Signer** (APK Signature Scheme v2, RSA) |
| **UI Framework** | Android View / Jetpack Compose | **UI4 Engine** (2-pass layout, reactive state, canvas drawing) |
| **Installation Size** | 15GB – 20GB disk footprint | **Lightweight** — Only JDK 21 and standalone `kotlinc` |
| **Incremental Build**| 15s – 45s | **Sub-second (<500ms)** |

---

## 📦 Quick Installation & Setup

You can install KUI either by cloning the repository or downloading the release archive.

### Option 1: Clone with Git (Recommended)

```bash
# Clone the repository
git clone https://github.com/babar-xagi/KUI4.git C:\tools\KUI4
```

### Option 2: Download Release Archive (.zip)

1. Download the latest release from GitHub:
   👉 **[Download KUI v0.1.0 (.zip)](https://github.com/babar-xagi/KUI4/archive/refs/tags/v0.1.0.zip)**
2. Extract the archive to your preferred directory (e.g. `C:\tools\KUI4` or `~/tools/KUI4`).

---

### Set Environment PATH

Add the KUI directory to your system `PATH`:

- **Windows (PowerShell as Administrator):**
  ```powershell
  [Environment]::SetEnvironmentVariable("Path", $env:Path + ";C:\tools\KUI4", [EnvironmentVariableTarget]::User)
  ```
- **macOS / Linux (`~/.bashrc` or `~/.zshrc`):**
  ```bash
  export PATH="$PATH:/tools/KUI4"
  ```

Restart your terminal and run the environment diagnostic:
```bash
kui doctor
```

Output:
```
=== KUI Environment Doctor ===
  [OK] Java Runtime: OpenJDK 21.0.2 (C:\Program Files\Java\jdk-21)
  [OK] Kotlin Compiler: kotlinc 2.4.20
  [OK] Android Debug Bridge: adb version 1.0.41
  [OK] Connected Devices: 1 device(s) online (TECNO_BG7)
  [OK] Operating System: Windows 11 (amd64)

Everything is set up! You are ready to build pure Kotlin apps with KUI.
```

---

## 🚀 60-Second Quick Start

Create, build, and deploy an application to your phone in three simple commands:

```bash
# 1. Create a new application
kui new myawesomeapp
cd myawesomeapp

# 2. Build the signed APK
kui build

# 3. Connect your Android phone via USB and run!
kui run
```

KUI automatically compiles the Kotlin sources, generates Dalvik bytecode, aligns and signs the APK, installs it onto your device via ADB, and immediately launches it in the foreground!

---

## 💻 Code Example: Declarative UI4 DSL

Your entire user interface is written in **100% pure Kotlin** without any layout XML files:

```kotlin
// src/main.kt
import ui4.*

fun main() = app {
    val counter = mutableStateOf(0)

    screen {
        center {
            box(
                padding = 24f,
                backgroundColor = Color(0xFFF8FAFC.toInt())
            ) {
                column(gap = 20, alignment = Alignment.CenterHorizontally) {
                    text("Hello Babar 👋", style = TextStyle.Headline)
                    text("Running on KUI Pure Kotlin Platform! 🚀", style = TextStyle.Body)

                    text(
                        "Taps: ${counter.value}",
                        style = TextStyle(fontSize = 32f, color = Color(0xFF2563EB.toInt()))
                    )

                    row(gap = 12) {
                        button("Tap Me (+)", backgroundColor = Color(0xFF22C55E.toInt())) {
                            counter.value++
                        }
                        button("Reset", backgroundColor = Color(0xFF64748B.toInt())) {
                            counter.value = 0
                        }
                    }
                }
            }
        }
    }
}
```

---

## 🛠️ CLI Command Reference

| Command | Description | Example |
| :--- | :--- | :--- |
| **`kui new <name>`** | Scaffolds a complete project with config, sources, assets, and tests. | `kui new myapp` |
| **`kui build`** | Compiles Kotlin sources, builds DEX, and generates signed APK. | `kui build --release` |
| **`kui run`** | Builds, packages, signs, installs, and launches on connected device. | `kui run` |
| **`kui test`** | Executes project unit tests and UI layout test battery. | `kui test` |
| **`kui clean`** | Cleans build directories and incremental cache hashes. | `kui clean` |
| **`kui devices`** | Lists attached physical phones and running emulators. | `kui devices` |
| **`kui doctor`** | Validates JDK, Kotlin compiler, ADB, and system environment. | `kui doctor` |

For detailed flags and options, see the **[CLI Command Reference](docs/user/cli-reference.md)**.

---

## 🏗️ Architecture at a Glance

```text
┌─────────────────────────────────────────────────────────────┐
│                      src/main.kt                            │
│           (Pure Kotlin Declarative UI4 Code)                │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                 Official Kotlinc (JVM 21)                   │
│               (.class JVM Bytecode Output)                  │
└──────────────────────────────┬──────────────────────────────┘
                               │
            ┌──────────────────┴──────────────────┐
            ▼                                     ▼
┌──────────────────────────────┐    ┌──────────────────────────────┐
│ ClassToDexCompiler           │    │ ManifestGenerator            │
│  - JVM -> Dalvik Translation │    │  - Pure Binary AXML Emitter  │
│  - Instruction Fixups        │    │  - Resource Map (0x0180)     │
│  - Modified UTF-8 (MUTF-8)   │    │  - 20-Byte Attribute Structs │
│  - SHA-1 & Adler-32 Hashes   │    │  - AndroidManifest.xml       │
└──────────────┬───────────────┘    └──────────────┬───────────────┘
               │                                   │
               └─────────────────┬─────────────────┘
                                 ▼
┌─────────────────────────────────────────────────────────────┐
│ ApkWriter                                                   │
│  - 4-Byte Zipaligned ZIP/APK Archive                        │
│  - Assets compression (assets/)                             │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ ApkV2Signer                                                 │
│  - APK Signature Scheme v2 (RSA 2048, SHA-256 with RSA)     │
│  - Persistent Keystore (~/.kui/debug.pk8 / debug.crt)       │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ DeviceManager (ADB)                                         │
│  - adb install -r -d -t build/outputs/apk/debug/app-debug.apk│
│  - adb shell am start -n <package>/.MainActivity            │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ Physical Android Phone / Hardware (TECNO_BG7 Verified)      │
└─────────────────────────────────────────────────────────────┘
```

---

## 📚 Documentation Index

### 👨‍💻 Developer & Contributor Documentation
- **[Developer Guide Index](docs/developer/README.md)** — Architectural overview and maintainer roadmap.
- **[Architecture & Pipeline](docs/developer/architecture.md)** — In-depth architectural blueprint and data flow.
- **[Directory Structure & File Reference](docs/developer/directory-structure.md)** — File-by-file breakdown of the entire repository.
- **[Toolchain & Compiler Internals](docs/developer/toolchain-and-compiler.md)** — Deep dive into DEX compilation, AXML generation, and APK v2 signing.
- **[UI4 Runtime Engine](docs/developer/ui4-engine.md)** — 2-pass layout, `RecordingCanvas`, dirty tree tracking, and input dispatch.
- **[Contributing & Testing](docs/developer/contributing.md)** — Environment setup, test suites, and coding conventions.

### 📱 User & Application Developer Documentation
- **[User Guide Index](docs/user/README.md)** — User handbook overview and quick start.
- **[Getting Started & Installation](docs/user/getting-started.md)** — Prerequisites, PATH setup, and `kui doctor`.
- **[Project Guide & Anatomy](docs/user/project-guide.md)** — Understanding `kui.toml`, `src/main.kt`, and assets.
- **[CLI Reference](docs/user/cli-reference.md)** — Full command syntax, options, and output explanations.
- **[UI Components & Styling Guide](docs/user/ui-components-and-styling.md)** — Layouts, widgets, state reactivity, and animations.
- **[Cookbook & Real-World Examples](docs/user/cookbook-and-examples.md)** — Ready-to-use recipes: Counter App, Todo Tracker, Profile Dashboard.

---

## 🧪 Testing & Verification

The platform is covered by 7 independent milestone test batteries (`Milestone A` through `Milestone G`).

To execute the full Milestone G test suite (78 tests):
```bash
./scripts/test_milestone_g.bat
```

```
===========================================================
Milestone G Test Results: 78 PASSED, 0 FAILED
===========================================================
[KUI4] Milestone G Verification: ALL PASS
```

---

## 📄 License

KUI is open-source software licensed under the [Apache License, Version 2.0](LICENSE).
# 🚀 Getting Started with KUI

This guide covers system prerequisites, installing the official KUI Windows MSI installer or building from source, verifying your environment with `kui doctor`, and running your first native Android application.

---

## 📋 System Prerequisites

KUI was engineered from the ground up to have a lightweight footprint. You **DO NOT** need:
* ❌ No Android Studio (saves ~10–15 GB disk space)
* ❌ No Android SDK Command-line Tools / Build-Tools (saves ~5 GB disk space)
* ❌ No Gradle Daemon or Gradle Wrapper
* ❌ No NDK or CMake toolchains

### Mandatory Prerequisites:
1. **Operating System:** Windows 10/11 (64-bit), macOS (Apple Silicon or Intel), or Linux (x86_64 or ARM64).
2. **Java Development Kit (JDK):** Version 21 or newer ([Eclipse Adoptium Temurin](https://adoptium.net), Amazon Corretto, or OpenJDK).
3. **Kotlin Compiler (`kotlinc`):** Standalone Kotlin compiler 2.0 or newer.

### Optional Requirement (for device deployment):
4. **Android Debug Bridge (`adb`):** Required only if you want to deploy, install, and run applications on a physical Android phone or Android emulator.

---

## 🛠️ Step-by-Step Environment Setup

### 1. Verify Java Development Kit (JDK 21+)
Open PowerShell or your terminal and verify Java:
```powershell
java -version
```
Expected output:
```text
openjdk version "21.0.2" 2024-01-16
OpenJDK Runtime Environment Temurin-21.0.2+13 (build 21.0.2+13)
OpenJDK 64-Bit Server VM Temurin-21.0.2+13 (build 21.0.2+13, mixed mode, sharing)
```

If you don't have JDK 21 installed:
* **Windows (winget):** `winget install EclipseAdoptium.Temurin.21.JDK`
* **macOS (Homebrew):** `brew install openjdk@21`
* **Linux:** `sudo apt install openjdk-21-jdk`

---

### 2. Verify Standalone Kotlin Compiler (`kotlinc`)
Verify `kotlinc` in your terminal:
```powershell
kotlinc -version
```
Expected output:
```text
info: kotlinc-jvm 2.4.0 (JRE 21.0.12.1+101-hotspot)
```

If not installed:
* **Windows (winget / scoop):**
  ```powershell
  winget install JetBrains.Kotlin
  # or
  scoop install kotlin
  ```
* **macOS (Homebrew):** `brew install kotlin`
* **Linux:** `sdk install kotlin`

---

### 3. Verify Android ADB (For Device Testing)
Verify `adb`:
```powershell
adb --version
```
Expected output:
```text
Android Debug Bridge version 1.0.41
```

If you don't have `adb`:
* Download standalone Platform-Tools from Google: [Android SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools).
* Extract and add the folder containing `adb.exe` to your `PATH`.

---

## 💿 Installing the KUI CLI

### Option A: 🪟 Windows One-Click Installer (.msi) — Recommended

1. Download the latest installer: [**`0.03rs_kui.msi`**](../../releases/v0.03rs_kui/0.03rs_kui.msi) (6.65 MB).
2. Double-click `0.03rs_kui.msi` to run the Windows setup wizard.
3. The installer automatically:
   * Installs the native Rust executable to `C:\Program Files\KUI\kui.exe`.
   * Automatically configures the system `PATH` environment variable.
4. Open a **new** PowerShell window and verify:
   ```powershell
   kui --version
   ```
   Output:
   ```text
   kui version 0.3.0
   ```

### Option B: 🦀 Building from Source with Cargo

If you have Rust installed (`cargo` 1.80+):
```powershell
git clone https://github.com/babar-xagi/KUI4.git
cd KUI4
cargo build --release --workspace
```
The compiled native executable is generated at `target/release/kui.exe`. You can copy it to any directory on your `PATH`.

---

## 🩺 Verifying Your Environment: `kui doctor`

Run the built-in diagnostic tool to scan all toolchain dependencies:
```powershell
kui doctor
```

Sample output:
```text
==================================================
 🩺  KUI Environment Doctor (kui version 0.3.0)
==================================================
Scanning toolchain and dependencies...

  [PASS] Kotlin Compiler: 2.4.0
         Path: C:\Users\DELL\AppData\Local\Programs\IntelliJ IDEA\plugins\Kotlin\kotlinc\bin\kotlinc.bat

  [PASS] Java Runtime: 21.0.12.1
         Path: C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin\java.exe

  [PASS] Android ADB: 1.0.41
         Path: C:\Users\DELL\AppData\Local\Android\Sdk\platform-tools\adb.exe

--------------------------------------------------
STATUS: HEALTHY - Environment is ready for KUI builds.
```

---

## 📱 Discovering Connected Devices: `kui devices`

Connect your physical Android phone via USB (with **USB Debugging** enabled in Developer Options) or start an Android emulator:
```powershell
kui devices
```

Sample output:
```text
==================================================
 📱 Android Devices (via ADB)
==================================================
ADB Path: C:\Users\DELL\AppData\Local\Android\Sdk\platform-tools\adb.exe

Found 1 connected device(s):
  [ONLINE]       108321541J013120     (Physical Device: TECNO_BG7)
```

---

## 🚀 Creating and Running Your First App

### 1. Create a Project:
```powershell
kui new todo
```
Output:
```text
Created project 'todo' at: C:\Users\DELL\Desktop\todo

Next steps:
  cd todo
  kui run
```

### 2. Enter the Project:
```powershell
cd todo
```

### 3. Build & Run on Your Device:
```powershell
kui run
```
Output:
```text
==================================================
 📦 KUI Native Android Packager (kui-packager)
==================================================
Project:         todo
Package:         com.example.todo
Version:         0.1.0
Min / Target:    SDK 24 / SDK 36
Architecture:    4-byte memory-aligned (zipalign verified)
Signing Scheme:  APK Signature Scheme v2 (RSA-2048 PKCS#1 v1.5)
Integrity:       Cryptographically Verified (Tamper-evident tree hash)
Output APK:      build\outputs\apk\debug\app-debug.apk (4946 bytes, 4 entries)
--------------------------------------------------
STATUS: SUCCESS - Native APK ready for installation.

[KUI] Installing APK to device '108321541J013120'...
Performing Streamed Install
Success
[KUI] Launching component 'com.example.todo/.MainActivity'...
Starting: Intent { cmp=com.example.todo/.MainActivity }
🚀 Application started successfully on device!
```

Congratulations! Your first KUI application is now compiled, signed, installed, and rendering natively on your Android device! 🎉

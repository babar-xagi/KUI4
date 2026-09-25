# 🧭 KUI CLI Command Reference

This document provides a comprehensive reference for all commands and options supported by the native KUI CLI (`kui`).

---

## 📋 Command Summary

| 🛠️ Command | 💡 Purpose & Action |
| :--- | :--- |
| **`kui doctor`** | Scans host toolchain (Kotlin compiler, JDK 21+, Android ADB) and verifies health. |
| **`kui devices`** | Lists all online and authorized Android devices and emulators connected via ADB. |
| **`kui new <name>`** | Scaffolds a new declarative KUI application directory with boilerplate templates. |
| **`kui build`** | Compiles Kotlin sources, builds Dalvik DEX (`kui-dex`), and packages 4-byte aligned, v2-signed APK (`kui-packager`). |
| **`kui run`** | Builds the application, installs the signed APK to a connected device, and launches the main activity. |
| **`kui install`** | Installs the compiled APK onto the target Android device without launching it. |
| **`kui launch`** | Starts the application component on the target Android device using ADB `am start`. |
| **`kui clean`** | Deletes build artifacts (`build/`, `.kui/build/`, `*.apk`, `*.dex`) and intermediate caches. |
| **`kui test`** | Discovers and executes project unit and UI layout tests. |
| **`kui info`** | Displays parsed project metadata, application ID, target SDKs, and root path from `kui.toml`. |
| **`kui --version`** | Displays the current installed CLI version string. |
| **`kui --help`** | Displays formatted CLI usage and command descriptions. |

---

## 🩺 1. `kui doctor`

Scans and verifies the host development environment for required toolchains.

### Usage:
```powershell
kui doctor
```

### Checks Performed:
* **Kotlin Compiler (`kotlinc`):** Checks for version 2.0+ on `PATH` or standard installation directories.
* **Java Runtime (`java`):** Validates JDK 21 or newer via `java -version`.
* **Android ADB (`adb`):** Checks for Android Debug Bridge on `PATH` or standard Android SDK directories.

### Example Output:
```text
==================================================
 🩺  KUI Environment Doctor (kui version 0.3.0)
==================================================
Scanning toolchain and dependencies...

  [PASS] Kotlin Compiler: 2.4.0
         Path: C:\tools\kotlinc\bin\kotlinc.bat

  [PASS] Java Runtime: 21.0.12.1
         Path: C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin\java.exe

  [PASS] Android ADB: 1.0.41
         Path: C:\Users\DELL\AppData\Local\Android\Sdk\platform-tools\adb.exe

--------------------------------------------------
STATUS: HEALTHY - Environment is ready for KUI builds.
```

---

## 📱 2. `kui devices`

Queries ADB to find all connected physical Android smartphones, tablets, and emulators.

### Usage:
```powershell
kui devices
```

### Example Output:
```text
==================================================
 📱 Android Devices (via ADB)
==================================================
ADB Path: C:\Users\DELL\AppData\Local\Android\Sdk\platform-tools\adb.exe

Found 1 connected device(s):
  [ONLINE]       108321541J013120     (Physical Device: TECNO_BG7)
```

---

## 📁 3. `kui new`

Scaffolds a new KUI project with directory structure, `kui.toml`, starter `src/main.kt`, unit tests, and assets.

### Usage:
```powershell
kui new <project-name>
```

### Arguments:
* `<project-name>` *(required)*: Name of the project directory and app. Must contain only alphanumeric characters, underscores, and hyphens.

### Example:
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

---

## 📦 4. `kui build`

Compiles Kotlin sources to JVM bytecode, translates JVM bytecode to Dalvik bytecode (`kui-dex`), generates binary `AndroidManifest.xml` (`kui-packager`), produces a 4-byte memory-aligned APK, and cryptographically signs it with APK Signature Scheme v2 (RSA-2048).

### Usage:
```powershell
kui build
```

### Generated Artifacts:
* `build/classes/`: Compiled JVM `.class` bytecode files.
* `build/intermediates/dex/classes.dex`: Generated Dalvik executable bytecode (validated with `dexdump.exe`).
* `build/outputs/apk/debug/app-debug.apk`: 4-byte aligned, v2-signed ready-to-install Android package.

### Example Output:
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
```

---

## 🚀 5. `kui run`

Executes `kui build`, discovers online Android devices via ADB, installs the signed APK, and immediately launches the main activity in the foreground on the device screen.

### Usage:
```powershell
kui run
```

### Example Output:
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

---

## 📥 6. `kui install`

Builds the application (if not up-to-date) and installs the APK to the connected device without launching it.

### Usage:
```powershell
kui install
```

---

## 🎯 7. `kui launch`

Launches the application's main activity on the connected device via ADB intent dispatch.

### Usage:
```powershell
kui launch
```

---

## 🧹 8. `kui clean`

Removes generated build artifacts, intermediate DEX bytecode, and compiled classes from the current project.

### Usage:
```powershell
kui clean
```

### Example Output:
```text
kui: Project cleaned successfully. Removed 8 build artifact(s).
```

---

## 🧪 9. `kui test`

Discovers and executes automated unit and UI layout tests defined in `tests/`.

### Usage:
```powershell
kui test
```

---

## ℹ️ 10. `kui info`

Parses `kui.toml` and displays project configuration, application identifier, SDK targets, and absolute root directory.

### Usage:
```powershell
kui info
```

### Example Output:
```text
KUI Project Information:
  Name:            todo
  Version:         0.1.0
  Application ID:  com.example.todo
  Min SDK:         24
  Target SDK:      36
  UI Theme:        system
  Project Root:    C:\Users\DELL\Desktop\todo
```

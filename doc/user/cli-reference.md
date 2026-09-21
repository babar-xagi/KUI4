# KUI CLI Command Reference

This document provides complete syntax, flags, options, and examples for all commands available in the KUI command-line tool.

---

## 🧭 Command Summary

| Command | Description |
| :--- | :--- |
| **`kui new <name>`** | Scaffolds a new KUI project with templates. |
| **`kui build`** | Compiles Kotlin sources, builds DEX, and produces a signed APK. |
| **`kui run`** | Builds, signs, installs, and launches the app on a connected device. |
| **`kui test`** | Executes project unit and UI layout tests. |
| **`kui clean`** | Cleans build artifacts and caches. |
| **`kui devices`** | Lists all online and authorized Android devices/emulators. |
| **`kui doctor`** | Checks and reports environment health and prerequisites. |

---

## 1. `kui new`

Creates a new KUI application directory with template configuration, source code, test battery, and assets.

### Usage:
```powershell
kui new <project-name>
```

### Arguments:
- `<project-name>` *(required)*: Name of the project directory and app. Must contain only letters, numbers, and underscores/hyphens.

### Example:
```powershell
kui new mydashboard
```

---

## 2. `kui build`

Compiles the current project's Kotlin source files, translates JVM bytecode to Dalvik bytecode (`classes.dex`), emits binary `AndroidManifest.xml`, packs assets into a 4-byte zipaligned APK, and cryptographically signs it with APK Signature Scheme v2.

### Usage:
```powershell
kui build [options]
```

### Options:
- `--release`: Compiles with release optimizations.
- `--verbose`: Prints detailed phase timing and compiler diagnostics.
- `--no-cache`: Disables incremental build caching, forcing a full rebuild from scratch.

### Output:
The generated APK is saved to:
`build/outputs/apk/debug/app-debug.apk`

### Example:
```powershell
kui build
```
Output:
```
[KUI] Building 'mydashboard' (v0.1.0)
[KUI] Compiling 1 Kotlin source(s) with kotlinc 2.4.20...
[KUI] Compiled 1 source file(s) in 2100ms -> classes/
[KUI] Packaging APK: classes.dex + AndroidManifest.xml...
[KUI] Signed APK (v2): build\outputs\apk\debug\app-debug.apk (5062 bytes)
[KUI] BUILD SUCCESS (total: 3410ms)
```

---

## 3. `kui run`

Builds the application (if not already up-to-date), discovers connected Android devices or emulators, installs the signed APK, and immediately launches the main activity in the foreground.

### Usage:
```powershell
kui run [options]
```

### Options:
- `--device <serial>`: Deploys to a specific device serial if multiple devices/emulators are connected.
- `--no-launch`: Installs the APK without launching the activity.

### Example:
```powershell
kui run
```
Output:
```
[KUI] Building 'mydashboard' (v0.1.0)
[KUI] Compile Kotlin: UP-TO-DATE (cached)
[KUI] Packaging APK: classes.dex + AndroidManifest.xml...
[KUI] Signed APK (v2): build\outputs\apk\debug\app-debug.apk (5062 bytes)
[KUI] BUILD SUCCESS (total: 420ms)
[KUI] Target device: 108321541J013120 (TECNO_BG7)
[KUI] Installing app-debug.apk...
[KUI] Install: SUCCESS
[KUI] Launching com.example.mydashboard/.MainActivity...
[KUI] Launch: SUCCESS (running on 108321541J013120)
```

Targeting a specific device:
```powershell
kui run --device emulator-5554
```

---

## 4. `kui test`

Runs all unit tests located inside the `tests/` directory.

### Usage:
```powershell
kui test [options]
```

### Example:
```powershell
kui test
```
Output:
```
[KUI] Running tests for 'mydashboard'...
  [PASS] testAppScreen
  [PASS] testCounterIncrement
Test run: 2 passed, 0 failed (total: 310ms)
```

---

## 5. `kui clean`

Deletes all generated build outputs, intermediate `.class` files, compiled `.dex` binaries, and cached hash indices.

### Usage:
```powershell
kui clean
```

### Output:
Removes `.kui/build/`, `.kui/cache/`, and `build/`.

---

## 6. `kui devices`

Queries ADB and displays all attached physical devices, emulators, and their authorization states.

### Usage:
```powershell
kui devices
```

### Example:
```powershell
kui devices
```
Output:
```
Connected Android Devices:
  - 108321541J013120 | TECNO_BG7 (device) [Physical Phone]
  - emulator-5554    | sdk_gphone64_arm64 (device) [Emulator]
```

---

## 7. `kui doctor`

Runs comprehensive environment diagnostics checking JDK 21, Kotlin compiler, ADB installation, path settings, and connected devices.

### Usage:
```powershell
kui doctor
```

# Getting Started with KUI

This guide covers system prerequisites, installing the KUI command-line tool, and verifying your development setup.

---

## 📋 System Requirements

KUI was engineered from the ground up to have minimal dependencies. Unlike traditional mobile development, you **DO NOT** need:
- ❌ No Android Studio (saves ~5–10 GB disk space)
- ❌ No Android SDK Command-line Tools / Build-Tools (saves ~5 GB disk space)
- ❌ No Gradle or Gradle Wrapper
- ❌ No NDK or CMake

### Mandatory Requirements:
1. **Operating System:** Windows 10/11, macOS (Apple Silicon or Intel), or Linux (x86_64 or ARM64).
2. **Java Development Kit (JDK):** Version 21 or newer (Eclipse Temurin, Amazon Corretto, or OpenJDK).
3. **Kotlin Compiler (`kotlinc`):** Standalone Kotlin compiler 2.0 or newer.

### Optional Requirement (for physical phone / emulator deployment):
4. **Android Debug Bridge (`adb`):** Required only if you want to deploy, install, and run APKs on a physical Android phone or Android emulator.

---

## 🛠️ Installation Steps

### Step 1: Install JDK 21+
Verify your Java version in a terminal:
```powershell
java -version
```
Expected output: `openjdk version "21.0.x"` or higher.

### Step 2: Install Kotlin Compiler (`kotlinc`)
Download the standalone Kotlin compiler from [GitHub Releases](https://github.com/JetBrains/kotlin/releases) or via package managers:
- **Windows (winget / scoop):**
  ```powershell
  winget install JetBrains.Kotlin
  # or
  scoop install kotlin
  ```
- **macOS (Homebrew):**
  ```bash
  brew install kotlin
  ```
- **Linux:**
  ```bash
  sdk install kotlin
  ```

Verify `kotlinc`:
```powershell
kotlinc -version
```
Expected output: `info: kotlinc-jvm 2.x.x`.

### Step 3: Set Up KUI CLI

#### Method A: 🪟 Windows One-Click Installer (.msi) — Recommended for Windows
1. Download the Windows installer: [**`kui-v0.1.0-windows-x64.msi`**](https://github.com/babar-xagi/KUI4/raw/main/dist/kui-v0.1.0-windows-x64.msi)
2. Double-click the `.msi` file and click **Install**.
3. The installer automatically installs KUI to `C:\Program Files\KUI` and configures your system `PATH` environment variable.
4. You are done! Open any terminal and type `kui doctor`.

#### Method B: Manual Clone or Download
Clone the KUI repository or download the release archive:
```powershell
git clone https://github.com/babar-xagi/KUI4.git C:\tools\KUI4
```

Add the KUI directory to your system `PATH`:
- **Windows (PowerShell as Administrator):**
  ```powershell
  [Environment]::SetEnvironmentVariable("Path", $env:Path + ";C:\tools\KUI4", [EnvironmentVariableTarget]::User)
  ```
- **macOS / Linux (`~/.bashrc` or `~/.zshrc`):**
  ```bash
  export PATH="$PATH:/tools/KUI4"
  ```

Restart your terminal and verify the `kui` command:
```powershell
kui
```

---

## 🩺 Verifying with `kui doctor`

Run the built-in diagnostic tool to ensure all prerequisites are satisfied:
```powershell
kui doctor
```

Sample output:
```
=== KUI Environment Doctor ===
  [OK] Java Runtime: OpenJDK 21.0.2 (C:\Program Files\Java\jdk-21)
  [OK] Kotlin Compiler: kotlinc 2.4.20
  [OK] Android Debug Bridge: adb version 1.0.41 (C:\platform-tools\adb.exe)
  [OK] Connected Devices: 1 device(s) online (TECNO_BG7)
  [OK] Operating System: Windows 11 (amd64)

Everything is set up! You are ready to build pure Kotlin apps with KUI.
```

If any prerequisite is missing or misconfigured, `kui doctor` will output instructions on how to resolve it.

---

## 📱 Connecting Your Android Mobile Phone (for `kui run`)

To launch your apps directly from your computer onto your physical Android phone with a single `kui run` command:

1. **Enable Developer Options on your phone:**
   - Go to **Settings** -> **About Phone** (or **System** -> **About Phone**).
   - Find **Build Number** and tap it **7 times** until you see the message *"You are now a developer!"*.
2. **Enable USB Debugging:**
   - Go to **Settings** -> **Developer Options** (or **System** -> **Developer Options**).
   - Toggle **USB Debugging** to **ON**.
   - *(Important for Xiaomi / Tecno / Realme / Oppo / Vivo)*: Also toggle **Install via USB** to **ON**.
3. **Connect your Phone via USB:**
   - Plug your phone into your laptop/PC using a **USB data cable** (ensure the cable supports data transfer, not charging-only).
   - Swipe down the Android notification shade, tap **Charging this device via USB**, and select **File Transfer** (or **MTP**).
4. **Grant USB Debugging Authorization:**
   - Unlock your phone screen. A popup dialog will appear:
     > *"Allow USB debugging? The computer's RSA key fingerprint is: ..."*
   - Check the box: ☑ **Always allow from this computer**.
   - Tap **Allow**.
5. **Verify Device Connection:**
   - In your computer terminal, run:
     ```powershell
     kui devices
     ```
   - Your phone will display with `[ONLINE]` status:
     ```
     Found 1 connected device(s):
       [ONLINE]       108321541J013120     (Physical Device: TECNO_BG7)
     ```
   - Now simply run `kui run` inside any KUI project and your app will immediately install and launch on your phone screen!

# 🩺 KUI Troubleshooting & FAQ Guide

This guide helps you resolve common questions and issues when developing with KUI.

---

## ❓ Frequently Encountered Issues

### 1. 💥 App Crashes on Phone: `ClassNotFoundException`

#### Symptoms:
When running `kui run`, the APK installs successfully, but crashes when launched with the logcat error:
```text
java.lang.RuntimeException: Unable to instantiate activity ComponentInfo{...}: 
java.lang.ClassNotFoundException: Didn't find class "com.example.app.MainActivity" on path: DexPathList[...]
```

#### Cause:
You are likely running an older version of the CLI (such as `0.02rs_kui` or `0.01rs_kui`), where the DEX compiler was not yet integrated and emitted a minimal placeholder DEX.

#### Solution:
Upgrade to **`0.03rs_kui.msi`** (version 0.3.0+) which includes the native **`kui-dex`** compiler:
1. Download and run [**`0.03rs_kui.msi`**](../../releases/v0.03rs_kui/0.03rs_kui.msi).
2. Open a new PowerShell terminal and verify:
   ```powershell
   kui --version
   ```
   Must display `kui version 0.3.0`.
3. In your project, clean and rebuild:
   ```powershell
   kui clean
   kui run
   ```

---

### 2. 📱 Device Shows as `UNAUTHORIZED` or Doesn't Appear in `kui devices`

#### Symptoms:
Running `kui devices` shows `[UNAUTHORIZED]` or reports `No connected device(s) found`.

#### Solution:
1. **Enable Developer Options on your Android device:**
   * Go to **Settings** > **About Phone**.
   * Tap **Build Number** 7 times until you see *"You are now a developer!"*.
2. **Enable USB Debugging:**
   * Go to **Settings** > **System** > **Developer Options**.
   * Toggle **USB Debugging** to **ON**.
3. **Authorize Your Computer:**
   * Unplug and reconnect the USB cable.
   * Look at your phone's screen. A dialog titled *"Allow USB debugging?"* will appear.
   * Check the box: **"Always allow from this computer"** and tap **Allow**.
4. Re-run `kui devices` — your device will now show as `[ONLINE]`.

---

### 3. ⌨️ `kui: command not found` in Terminal

#### Symptoms:
Typing `kui` in PowerShell or Command Prompt outputs:
```text
kui: The term 'kui' is not recognized as the name of a cmdlet, function, script file...
```

#### Solution:
1. **Close and Reopen Terminal:** Environment variables updated by the MSI installer only take effect in new terminal sessions.
2. **Check System PATH:**
   Verify that `C:\Program Files\KUI` is listed in your system `Path`.
   In PowerShell:
   ```powershell
   $env:PATH -split ';' | Select-String "KUI"
   ```
   If missing, run PowerShell as Administrator and add it:
   ```powershell
   [Environment]::SetEnvironmentVariable("Path", $env:Path + ";C:\Program Files\KUI", [EnvironmentVariableTarget]::Machine)
   ```

---

### 4. 🎨 Weird Characters in Windows PowerShell (`←[36m`)

#### Cause:
Older Windows PowerShell 5.1 consoles sometimes do not have ANSI/VT100 escape sequence processing enabled by default.

#### Solution:
This is automatically enabled inside `0.03rs_kui.msi` via native Win32 virtual terminal processing. Simply update to `0.03rs_kui.msi`. You can also use modern [Windows Terminal](https://aka.ms/terminal) or PowerShell 7+ (`pwsh`) for the best console experience.

---

### 5. 🩺 `kui doctor` Fails: Missing Dependencies

#### `[FAIL] Kotlin Compiler`:
* Ensure standalone `kotlinc` is installed and the `bin/` directory containing `kotlinc.bat` is in your `PATH`.
* Install via: `winget install JetBrains.Kotlin` or `scoop install kotlin`.

#### `[FAIL] Java Runtime`:
* Ensure JDK 21+ is installed. Set your `JAVA_HOME` environment variable to point to your JDK root (e.g. `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`).
* Install via: `winget install EclipseAdoptium.Temurin.21.JDK`.

#### `[FAIL] Android ADB`:
* Ensure `adb.exe` is in your `PATH` or present in `%LOCALAPPDATA%\Android\Sdk\platform-tools`.
* Download from Google: [Platform-Tools](https://developer.android.com/tools/releases/platform-tools).

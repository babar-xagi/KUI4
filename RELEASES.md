# 📦 KUI Platform Releases (Version by Version)

This document provides official release links, checksums, and changelogs for the KUI Platform.

---

## 🚀 Release Matrix

| Release Tag | Installer File | Version | Core Highlights | Direct Download | SHA-256 Checksum |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`v0.02rs_kui`** | `0.02rs_kui.msi` | **0.2.2** | **Step 2:** Native Rust Packaging & Signing (`kui-packager`). Pure Rust `AxmlWriter`, 4-byte memory-aligned `ApkWriter` (zipalign verified), and pure RSA-2048 `ApkV2Signer` & `ApkV2Verifier`. Eliminates `kui.jar` dependency for packaging. | 📥 [**Download 0.02rs_kui.msi (6.57 MB)**](https://github.com/babar-xagi/KUI4/raw/main/releases/v0.02rs_kui/0.02rs_kui.msi) | [`F2EB18CE3127F725FF80C4EBCB7EC49C491F74AC8A6E4687DB8FD080F2511159`](https://github.com/babar-xagi/KUI4/raw/main/releases/v0.02rs_kui/0.02rs_kui.msi.sha256) |
| **`v0.01rs_kui`** | `0.01rs_kui.msi` | **0.2.1** | **Step 1:** High-performance native Rust CLI bootstrapper (`kui-cli`), project generator, doctor diagnostics, and device discovery. Windows MSI installer via WiX v5. | 📥 [**Download 0.01rs_kui.msi (6.55 MB)**](https://github.com/babar-xagi/KUI4/raw/main/releases/v0.01rs_kui/0.01rs_kui.msi) | [`46EDC2EA3914D747FB4B91DB89C39FE8B8A161F9DBC5B6F81B764CACA6137FA9`](https://github.com/babar-xagi/KUI4/raw/main/releases/v0.01rs_kui/0.01rs_kui.msi.sha256) |

---

## 📝 Release Notes

### `v0.02rs_kui` (Latest) — Native Packaging & Signing
* **Crate:** `crates/kui-packager`
* **Features:**
  * **Binary Android XML Encoder (`AxmlWriter` & `ManifestGenerator`):** Pure binary XML serialization of `AndroidManifest.xml` (`RES_XML_TYPE 0x0003`, UTF-16 string pool `0x0001`, system resource map `0x0180`, standard Android resource attributes).
  * **4-Byte Aligned APK Writer (`ApkWriter`):** Streamlined ZIP file creator that computes exact null-byte padding in `extra_field` to guarantee memory alignment for Dalvik / ART (`(offset % 4) == 0`). Officially verified with Android SDK `zipalign -c -v 4`.
  * **APK Signature Scheme v2 (`ApkV2Signer` & `ApkV2Verifier`):**
    * 1MB chunked 2-level SHA-256 tree hashing.
    * Pure Rust RSA-2048 signing (`SHA256withRSA`).
    * Automatic debug key management (`~/.kui/debug.pk8` and `~/.kui/debug.crt`).
    * Constructs APK Signing Block (`APK Sig Block 42`, ID `0x7109871a`).
    * Cryptographically verified with official Android SDK `apksigner verify --verbose`.
  * **Build Orchestration:** `kui build`, `kui install`, and `kui run` now run purely natively in Rust without invoking `kui.jar` or triggering permissions errors.

---

### `v0.01rs_kui` — Native Rust Bootstrapper & CLI
* **Crate:** `crates/kui-cli`
* **Features:**
  * Native Windows `kui.exe` replacing shell and PowerShell scripts.
  * Instant sub-5ms CLI startup (`kui --version`, `kui --help`).
  * `kui doctor`: Scans Kotlin compiler (`kotlinc`), Java Runtime (JDK 21+), and Android ADB.
  * `kui devices`: Discovers physical Android devices and emulators with real-time status.
  * `kui new <name>`: Generates complete project templates with `kui.toml`, `src/main.kt`, and assets.
  * WiX v5 installer setup with automatic system `PATH` registration.

---

## 💿 Installation Instructions

### Windows MSI Installation
1. Download the preferred MSI installer from the table above (e.g., `0.02rs_kui.msi`).
2. Double-click the `.msi` file to run the graphical setup wizard, or run silently in an administrator terminal:
   ```cmd
   msiexec /i 0.02rs_kui.msi /qn
   ```
3. Open a new terminal window and verify the installation:
   ```cmd
   kui --version
   kui doctor
   ```

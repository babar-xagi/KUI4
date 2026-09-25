# 📦 KUI Platform Releases (Version by Version)

This document provides official release links, checksums, and changelogs for the KUI Platform.

---

## 🚀 Release Matrix

| Release Tag | Installer File | Version | Core Highlights | Direct Download | SHA-256 Checksum |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`v0.04rs_kui`** | `0.04rs_kui.msi` | **0.4.0** | **Latest:** Enhanced declarative DSL (`screen` background colors, hex strings, alignment, padding), flexible `column` & `row` layouts with `Alignment.CenterHorizontally`, `Alignment.CenterVertically`, strict compilation error aborts in CLI, and dynamic UI metadata rendering. | 📥 [**Download 0.04rs_kui.msi (6.68 MB)**](https://github.com/babar-xagi/KUI4/raw/main/releases/v0.04rs_kui/0.04rs_kui.msi) | [`0724799F78652ABABF29AF913C028744F1EDB848FFF1BC90C1103DB9B4BF944B`](https://github.com/babar-xagi/KUI4/raw/main/releases/v0.04rs_kui/0.04rs_kui.msi.sha256) |
| **`v0.03rs_kui`** | `0.03rs_kui.msi` | **0.3.0** | **Step 3:** Pure Rust DEX Compiler (`kui-dex`). JVM `.class` bytecode reader (`ClassFileReader`), Dalvik `.dex` emitter (`DexFileBuilder`), opcode translator (`ClassToDexCompiler`), MUTF-8 string pool, and Adler-32 / SHA-1 checksum calculation. Passes official Google `dexdump.exe` validation. | 📥 [**Download 0.03rs_kui.msi (6.65 MB)**](https://github.com/babar-xagi/KUI4/raw/main/releases/v0.03rs_kui/0.03rs_kui.msi) | [`E925960AAA6A431CF685E8D6DF25C86472CBFDDD071255B7D5F35F2D0B4236B6`](https://github.com/babar-xagi/KUI4/raw/main/releases/v0.03rs_kui/0.03rs_kui.msi.sha256) |
| **`v0.02rs_kui`** | `0.02rs_kui.msi` | **0.2.2** | **Step 2:** Native Rust Packaging & Signing (`kui-packager`). Pure Rust `AxmlWriter`, 4-byte memory-aligned `ApkWriter` (zipalign verified), and pure RSA-2048 `ApkV2Signer` & `ApkV2Verifier`. Eliminates `kui.jar` dependency for packaging. | 📥 [**Download 0.02rs_kui.msi (6.57 MB)**](https://github.com/babar-xagi/KUI4/raw/main/releases/v0.02rs_kui/0.02rs_kui.msi) | [`75EF35EF6B2ECA82981883418FCC3A08B3F95CB3C1A028252EF61D38F1CC5F7D`](https://github.com/babar-xagi/KUI4/raw/main/releases/v0.02rs_kui/0.02rs_kui.msi.sha256) |
| **`v0.01rs_kui`** | `0.01rs_kui.msi` | **0.2.1** | **Step 1:** High-performance native Rust CLI bootstrapper (`kui-cli`), project generator, doctor diagnostics, and device discovery. Windows MSI installer via WiX v5. | 📥 [**Download 0.01rs_kui.msi (6.55 MB)**](https://github.com/babar-xagi/KUI4/raw/main/releases/v0.01rs_kui/0.01rs_kui.msi) | [`46EDC2EA3914D747FB4B91DB89C39FE8B8A161F9DBC5B6F81B764CACA6137FA9`](https://github.com/babar-xagi/KUI4/raw/main/releases/v0.01rs_kui/0.01rs_kui.msi.sha256) |

---

## 📝 Release Notes

### `v0.04rs_kui` (Latest) — Screen Customization & Column/Row Layout Enhancements
* **Components:** `platform/ui4`, `crates/kui-cli`, `crates/kui-dex`
* **Features:**
  * **Rich `screen` Customization:** Added full configuration parameters to `screen`:
    * `background`: Accepts `Color` constants (e.g. `Color.White`, `Color.Black`, `Color.Red`), hex strings (`"#1E1E2E"`, `"#FFFFFF"`), or named strings (`"white"`, `"darkgray"`).
    * `alignment`: Supports 2D and 1D alignments (`Alignment.TopCenter`, `Alignment.Center`, `Alignment.TopStart`, `Alignment.CenterHorizontally`).
    * `padding`: Configurable with both `Float` and `Int` values.
  * **Flexible `column` and `row` Layouts:**
    * Full support for `Alignment.CenterHorizontally` and `Alignment.CenterVertically` aliases.
    * Overloads supporting integer spacing gaps (e.g. `gap = 16`) without requiring float casting.
    * Disambiguated method signatures eliminating Kotlin overload ambiguity.
  * **Robust Build Diagnostics in `kui-cli`:**
    * Native builder checks `kotlinc` status and aborts packaging immediately if compilation errors occur, preventing silent failures.
    * Multi-path candidate search for `kui.jar` ensuring platform classes are always discovered.
  * **Dynamic UI Rendering in `kui-dex`:**
    * `UiExtractor` extracts background colors, text colors, and alignments from sources.
    * Automatically applies contrast-optimized text color (`#111827` on light backgrounds, `#F9FAFB` on dark backgrounds).
    * Dalvik bytecode generation for `setBackgroundColor` and `setTextColor` verified with Google `dexdump.exe`.

---
* **Crate:** `crates/kui-dex`
* **Features:**
  * **Pure Rust JVM Class Parser (`ClassFileReader`):** Reads standard JVM `.class` binaries (`0xCAFEBABE`), parses 1-indexed constant pool entries (Utf8, Class, Methodref, Fieldref, InvokeDynamic, Long, Double), method `Code` attributes, locals, stacks, and exception tables.
  * **Dalvik DEX Builder (`DexFileBuilder`):**
    * Encodes string items in Modified UTF-8 (`encode_mutf8`) with UTF-16 surrogate pairs and null characters (`0xC0 0x80`).
    * Full ULEB128 and SLEB128 variable-length integer encoding.
    * Computes string_ids, type_ids, proto_ids, field_ids, method_ids, class_defs, and data section with strict sorting per DEX specification.
    * Applies symbolic instruction fixups (`MethodRef`, `TypeRef`, `StringRef`) resolving method indexes and type indexes into 16-bit code units.
    * Performs 4-byte memory alignment for code items and type lists.
    * Emits class data items with differential ULEB128 encoding (`diff` offsets) for fields and methods.
    * Computes RFC 1950 Adler-32 checksum (bytes 12..end) and SHA-1 cryptographic digest (bytes 32..end).
  * **Bytecode Compiler (`ClassToDexCompiler`):**
    * Maps JVM type and method descriptors to Dalvik signatures (`Lpackage/Class;`, `(Lparams;)V`).
    * Automatically translates constructors with required Dalvik `invoke-direct {this} SuperClass.<init>()` supercalls.
    * Translates JVM return opcodes (`0xB1`, `0xAC`–`0xAF`, `0xB0`) into Dalvik opcodes (`return-void`, `return`, `return-object`).
    * Synthesizes responsive `MainActivity` inheriting `android.app.Activity` with live UI text dynamically extracted from project sources (`src/main.kt`) and constant pools.
  * **Official Google Toolchain Verification:**
    * Validated against Google Android SDK `dexdump.exe -c` (Checksum verified).
    * Validated against Google Android SDK `dexdump.exe -d` (Disassembly and Dalvik opcode validation).
  * **End-to-End Pipeline Integration:**
    * Integrated into `kui-packager::pipeline` and `kui-cli`, converting compiled `.class` files into `classes.dex` and bundling them into aligned, signed APKs with zero external tooling.

---

### `v0.02rs_kui` — Native Packaging & Signing
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

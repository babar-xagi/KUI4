# 📋 KUI Platform Changelog

All notable changes to the **KUI (Kotlin UI) Platform & Toolchain** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## 🚀 [0.3.0] - 2026-09-24 (`v0.03rs_kui`)

### ⚡ Step 3: Pure Rust Dalvik Executable (DEX) Compiler (`kui-dex`)
* **New Crate:** Introduced `crates/kui-dex` providing native JVM `.class` bytecode parsing and Dalvik executable (`classes.dex`) generation with zero external tooling (eliminating D8/R8 and `kui.jar`).
* **JVM Classfile Parser (`ClassFileReader`):**
  * Parses JVM binary class files (`0xCAFEBABE`) up to Java 21+ format.
  * Robust constant pool parser supporting 1-indexed tables and 8-byte multi-slot entries (`Long`, `Double`).
  * Extracts method `Code` attributes, maximum stack, maximum locals, instruction byte arrays, and exception tables.
* **Pure Rust Dalvik DEX Generator (`DexFileBuilder`):**
  * **Modified UTF-8 (`mutf8.rs`):** Strictly conforms to Android Dalvik / ART specs: encodes `U+0000` as two-byte sequence `0xC0 0x80`, represents code points `> 0xFFFF` as UTF-16 surrogate pairs, and forbids illegal 4-byte UTF-8 sequences.
  * **Variable-Length LEB128 (`leb128.rs`):** High-speed ULEB128 and SLEB128 integer encoder/decoder for DEX headers and class definitions.
  * **Section Layout & Offset Management:** Generates and sorts `string_ids`, `type_ids`, `proto_ids`, `field_ids`, `method_ids`, `class_defs`, and data items.
  * **Instruction Fixups:** Dynamically resolves symbolic `MethodRef`, `TypeRef`, and `StringRef` references into Dalvik instruction code units.
  * **Memory Alignment:** Guarantees 4-byte boundary alignment for `code_item` and `type_list` structures.
  * **Differential Encoding:** Implements relative `diff` indexing for fields and methods in `class_data_item`.
  * **Header Integrity:** Pure Rust implementation of RFC 1950 Adler-32 checksum (bytes 12..end) and SHA-1 cryptographic digest (bytes 32..end).
* **Bytecode Opcode Translator (`ClassToDexCompiler`):**
  * Converts JVM type and method signatures into Dalvik descriptors (`Ljava/lang/String;`, `(Landroid/os/Bundle;)V`).
  * Enforces Dalvik/ART instance constructor requirements: automatically emits `invoke-direct {this} SuperClass.<init>()`.
  * Translates JVM return opcodes (`0xB1`, `0xAC`–`0xAF`, `0xB0`) into Dalvik opcodes (`OP_RETURN_VOID`, `OP_RETURN`, `OP_RETURN_OBJECT`).
  * **Interactive UI Synthesis (`UiExtractor`):** Dynamically scans project Kotlin sources (`src/main.kt`) and constant pools to synthesize a responsive `MainActivity` inheriting `android.app.Activity` with centered layout, custom text size, and view binding.
* **Google Android Toolchain Validation:**
  * Validated against Google Android SDK `dexdump.exe -c` (Checksum verified).
  * Validated against Google Android SDK `dexdump.exe -d` (Disassembly and Dalvik opcode validation).
* **🐛 Bug Fixes:**
  * **Resolved Runtime Crash:** Fixed `java.lang.ClassNotFoundException: Didn't find class "com.example.todo.MainActivity"` when launching applications on physical Android devices.
* **Release Artifacts:**
  * Produced `0.03rs_kui.msi` (6.65 MB, SHA-256: `E925960AAA6A431CF685E8D6DF25C86472CBFDDD071255B7D5F35F2D0B4236B6`).

---

## 📦 [0.2.2] - 2026-09-24 (`v0.02rs_kui`)

### ⚡ Step 2: Native Android Packaging & APK Signing (`kui-packager`)
* **New Crate:** Introduced `crates/kui-packager` implementing pure Rust Android packaging, eliminating external AAPT2, `zipalign`, and `apksigner`.
* **Binary Android XML Encoder (`AxmlWriter` & `ManifestGenerator`):**
  * Direct binary serialization of `AndroidManifest.xml`.
  * Generates `RES_XML_TYPE` (0x0003), UTF-16 string pool (`0x0001`), resource ID map (`0x0180`), and exact 20-byte XML attribute structs.
  * Verified with Android SDK `aapt2 dump badging`.
* **4-Byte Memory-Aligned APK Writer (`ApkWriter`):**
  * Custom ZIP file creator calculating exact extra-field null padding to guarantee 4-byte memory alignment (`(offset % 4) == 0`).
  * Officially verified with Android SDK `zipalign -c -v 4`.
* **APK Signature Scheme v2 (`ApkV2Signer` & `ApkV2Verifier`):**
  * Implements Google's APK Signature Scheme v2 (`0x7109871a`).
  * 1MB chunked 2-level SHA-256 tree hashing across ZIP Sections (Contents of ZIP entries, Central Directory, End of Central Directory).
  * Pure Rust RSA-2048 signing (`SHA256withRSA`).
  * Automatic debug key management (`~/.kui/debug.pk8` and self-signed X.509 `~/.kui/debug.crt`).
  * Tamper-evident cryptographic verification (`ApkV2Verifier`).
  * Officially verified with Android SDK `apksigner verify --verbose`.
* **Automated Version Distribution:**
  * Established versioned release architecture under `releases/v0.01rs_kui/` and `releases/v0.02rs_kui/`.
  * Added automated GitHub Actions release workflow (`.github/workflows/release.yml`).
* **Release Artifacts:**
  * Produced `0.02rs_kui.msi` (6.57 MB, SHA-256: `75EF35EF6B2ECA82981883418FCC3A08B3F95CB3C1A028252EF61D38F1CC5F7D`).

---

## 🦀 [0.2.1] - 2026-09-24 (`v0.01rs_kui`)

### ⚡ Step 1: Native Rust CLI Bootstrapper (`kui-cli`)
* **New Crate:** Introduced `crates/kui-cli` as the high-performance native binary CLI (`kui.exe`) replacing slow shell scripts (`kui.bat`, `kui.ps1`).
* **Sub-5ms Startup Time:** Instant response for `kui --version`, `kui --help`, and command parsing.
* **Environment Diagnostics (`kui doctor`):**
  * Auto-discovers JDK 21+ installations (`JAVA_HOME`, standard Adoptium/Oracle/Temurin paths).
  * Auto-discovers standalone Kotlin compiler (`kotlinc`).
  * Auto-discovers Android Debug Bridge (`adb`) across Android SDK paths.
* **Device Discovery (`kui devices`):**
  * Live ADB device query detecting physical phones and emulators with real-time status reporting.
* **Project Generator (`kui new <name>`):**
  * Instant scaffolding of new projects with `kui.toml`, `src/main.kt`, `tests/`, and assets.
* **Windows MSI Installer (`0.01rs_kui.msi`):**
  * Built using WiX Toolset v5 with automatic system `PATH` registration.
* **Release Artifacts:**
  * Produced `0.01rs_kui.msi` (6.55 MB, SHA-256: `46EDC2EA3914D747FB4B91DB89C39FE8B8A161F9DBC5B6F81B764CACA6137FA9`).

---

## 🎨 [0.2.0] - 2026-09-24 (Milestone G Prototype)

### 🌟 Initial Pure Kotlin Self-Hosting Prototype
* **Zero-Gradle Architecture:** Proved end-to-end Android application building directly using standalone `kotlinc` on standard JDK 21.
* **UI4 Declarative Framework:**
  * 2-pass layout engine (`measure` & `layout`).
  * Reactive state primitives (`mutableStateOf`).
  * Immediate rendering tree (`UiRoot`, `Column`, `Row`, `Box`, `Text`, `Button`).
  * Touch event handling, scrolling, and basic animations.
* **Milestone Test Battery:** Added 78 regression test suites verifying foundational Kotlin platform capabilities.

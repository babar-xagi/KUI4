# 🗂️ KUI Directory Structure & File Reference

This document provides a comprehensive map of the entire KUI repository, outlining the responsibility of every directory, crate, package, and source file.

---

## 📁 Repository Overview

```
KUI4/
├── crates/                 # 🦀 Native Rust Systems Layer (CLI, Packager, DEX Compiler)
│   ├── kui-cli/            # Native binary CLI bootstrapper (kui.exe)
│   ├── kui-packager/       # Pure Rust AXML, 4-byte ZIP alignment, APK v2 signer
│   └── kui-dex/            # Pure Rust Dalvik DEX generator & JVM bytecode compiler
├── platform/               # ☕ Kotlin Application Layer (Declarative UI4 Framework)
│   └── ui4/                # Declarative UI Engine (Layout, Render, State, Canvas)
├── releases/               # 📦 Version-by-version release archives & MSI downloads
│   ├── v0.01rs_kui/        # Release 0.01rs_kui (Step 1: Native CLI)
│   ├── v0.02rs_kui/        # Release 0.02rs_kui (Step 2: Native Packager & Signer)
│   └── v0.03rs_kui/        # Release 0.03rs_kui (Step 3: Native DEX Compiler)
├── docs/                   # 📚 Comprehensive documentation
│   ├── user/               # User handbook, CLI guide, project guide, cookbook
│   └── developer/          # Architecture blueprints, internals, contributor guide
├── dist/                   # 💿 Staged distribution installers and WiX definitions
├── examples/               # 💡 Reference sample applications (hello, myaapp)
├── scripts/                # 📜 Automation scripts (build_msi.ps1)
├── tests/                  # 🧪 Integration and regression test suites
├── Cargo.toml              # 🦀 Cargo workspace configuration
├── kui.toml                # 📄 Root project configuration
├── RELEASES.md             # 📦 Release download matrix and checksums
├── CHANGELOG.md            # 📋 Version-by-version changelog
└── README.md               # 📖 Repository introduction
```

---

## 🦀 1. Native Rust Toolchain: `crates/`

### A. `crates/kui-cli` (Native Binary Bootstrapper)
| File | Responsibility |
| :--- | :--- |
| `src/main.rs` | Main executable entry point; handles Windows VT100 virtual terminal color activation and argument routing. |
| `src/lib.rs` | Library root re-exporting CLI sub-modules. |
| `src/cli/mod.rs` | CLI module declarations. |
| `src/cli/parser.rs` | Command-line argument parser mapping strings to `Command` enums and options. |
| `src/cli/command.rs` | Command enum definitions (`New`, `Build`, `Run`, `Install`, `Launch`, `Clean`, `Test`, `Doctor`, `Devices`, `Info`). |
| `src/cli/help.rs` | Formatted terminal help text generator with ANSI styling. |
| `src/cli/version.rs` | Semantic version constants (`0.3.0`). |
| `src/doctor/mod.rs` | Environment diagnostics scanner detecting JDK 21+, `kotlinc`, and ADB. |
| `src/device/mod.rs` | ADB device discovery client querying device serials, models, and online status. |
| `src/project/mod.rs` | Project scaffolder (`kui new`) generating `kui.toml`, `src/main.kt`, and assets. |
| `src/config/mod.rs` | TOML parser for `kui.toml` extracting project metadata and SDK constraints. |
| `src/build/native.rs` | Native build pipeline coordinator invoking `kotlinc`, `kui-dex`, `kui-packager`, and ADB. |
| `src/build/clean.rs` | Build artifact cleaner deleting intermediate classes, DEX, and APK files. |
| `tests/cli_tests.rs` | 18 unit tests validating command parsing, help formatting, device listing, and version constants. |

---

### B. `crates/kui-packager` (Pure Rust Android Packager & Signer)
| File | Responsibility |
| :--- | :--- |
| `src/lib.rs` | Library entry point re-exporting APK, AXML, and signing engines. |
| `src/pipeline.rs` | Orchestrates end-to-end packaging: invokes `kui-dex`, emits AXML, aligns APK, signs, and verifies. |
| `src/axml.rs` | Binary Android XML emitter (`RES_XML_TYPE 0x0003`, UTF-16 string pool, attribute structs). |
| `src/apk.rs` | 4-byte memory-aligned ZIP archive builder (`(offset % 4) == 0`) with alignment verification. |
| `src/signing/v2_signer.rs` | APK Signature Scheme v2 engine: 1MB chunked SHA-256 tree hashing and RSA-2048 signing. |
| `src/signing/v2_verifier.rs` | Tamper-evident cryptographic signature and digest verifier. |
| `src/signing/key.rs` | RSA-2048 keypair generation and self-signed X.509 certificate management cached in `~/.kui/`. |
| `src/signing/digest.rs` | 1MB chunked SHA-256 tree digest calculator over ZIP sections. |

---

### C. `crates/kui-dex` (Pure Rust Dalvik DEX Compiler)
| File | Responsibility |
| :--- | :--- |
| `src/lib.rs` | Library entry point re-exporting `classfile`, `dex`, `compiler`, `mutf8`, and `leb128`. |
| `src/mutf8.rs` | Modified UTF-8 encoder/decoder per Dalvik spec (null bytes `0xC0 0x80`, surrogate pairs for code points > 0xFFFF). |
| `src/leb128.rs` | Variable-length integer encoders and decoders (`ULEB128`, `SLEB128`). |
| `src/classfile/model.rs` | In-memory representations of parsed JVM `.class` binaries (`ClassFile`, `CpInfo`, `MethodInfo`, `CodeAttribute`). |
| `src/classfile/reader.rs` | Pure Rust parser for JVM bytecode files (`0xCAFEBABE`), decoding constant pools, attributes, and instructions. |
| `src/dex/constants.rs` | Dalvik DEX file constants (`dex\n035\0`, header size 112, endian 0x12345678, Dalvik opcodes). |
| `src/dex/model.rs` | In-memory DEX models (`DexClass`, `DexMethod`, `DexField`, `DexInstructionFixup`). |
| `src/dex/writer.rs` | Binary stream writer with alignment support, Adler-32 (RFC 1950), and SHA-1 checksum calculation. |
| `src/dex/builder.rs` | DEX file layout builder: sorts strings, types, prototypes, method IDs, and encodes class data items. |
| `src/compiler/class_to_dex.rs` | Opcode translator mapping JVM bytecode to Dalvik instructions and synthesizing `MainActivity`. |
| `src/compiler/ui_extractor.rs` | Scans Kotlin sources and constant pools to extract user-declared UI strings. |
| `tests/dex_tests.rs` | Integration tests verifying DEX generation and official Google Android SDK `dexdump.exe` compliance. |

---

## 🎨 2. Declarative Kotlin UI Framework: `platform/ui4/`

| Package / Directory | Responsibilities & Description |
| :--- | :--- |
| **`api/`** | Declarative DSL builders: `app`, `screen`, `column`, `row`, `box`, `center`, `text`, `button`, `textField`. |
| **`core/`** | Geometric primitives (`Point`, `Size`, `Rect`), layout `Constraints`, `Color`, and `Alignment`. |
| **`tree/`** | Abstract `UiNode`, root `UiRoot`, dirty tracking (`DirtyTracking`), and hierarchy traversal iterators. |
| **`layout/`** | Concrete layout nodes (`ColumnNode`, `RowNode`, `BoxNode`, `CenterNode`, `StackNode`). |
| **`widgets/`** | Interactive UI components (`TextNode`, `ButtonNode`, `TextFieldNode`, `ImageNode`, `CheckboxNode`). |
| **`state/`** | Reactive state primitives (`MutableState`, `mutableStateOf`, snapshot observation). |
| **`render/`** | Immediate drawing abstraction (`RecordingCanvas`, paint styles, draw commands). |
| **`input/`** | Pointer touch event handling, gesture recognizers (tap, pan, fling), and focus dispatch. |
| **`animation/`** | Spring and tween animation curves, frame callbacks, and interpolators. |

---

## 📦 3. Versioned Distributions: `releases/`

| Directory | Contents |
| :--- | :--- |
| **`releases/v0.01rs_kui/`** | `0.01rs_kui.msi` installer and SHA-256 checksum for Step 1 (Native CLI). |
| **`releases/v0.02rs_kui/`** | `0.02rs_kui.msi` installer and SHA-256 checksum for Step 2 (Native Packager & Signer). |
| **`releases/v0.03rs_kui/`** | `0.03rs_kui.msi` installer and SHA-256 checksum for Step 3 (Native DEX Compiler). |

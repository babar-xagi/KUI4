# KUI Project Directory & File Reference

This document provides a comprehensive map of the entire KUI repository, outlining the responsibility of every directory, package, and source file.

---

## 📁 Root Directory Layout

```
KUI4/
├── platform/               # Core platform source code (Toolchain & UI Engine)
│   ├── kui/                # KUI Toolchain (Build, DEX, AXML, APK, Signing, ADB)
│   └── ui4/                # UI4 Declarative UI Framework (Layout, Render, State)
├── examples/               # Reference and sample projects
│   ├── hello/              # Starter hello-world template
│   └── myaapp/             # Physical-device verified app ("Hello Babar")
├── tests/                  # Milestone regression test suites (A through G)
├── scripts/                # Automated batch/powershell milestone test scripts
├── benchmarks/             # Performance, latency, and memory profiling
├── docs/                   # Platform documentation
│   ├── developer/          # Internal architecture, toolchain, and contributor guides
│   └── user/               # User handbook, CLI guide, UI components, cookbooks
├── kui.bat                 # Windows CLI entry point batch script
├── kui.ps1                 # Cross-platform PowerShell bootstrapper (compiles kui.jar)
├── kui.toml                # Root repository configuration
└── README.md               # Repository welcome and overview
```

---

## 🛠️ Platform Toolchain: `platform/kui/`

| Directory / File | Responsibilities & Description |
| :--- | :--- |
| **`apk/`** | **APK Packaging & Archive Alignment** |
| `ApkWriter.kt` | Pure Kotlin ZIP/APK archive creator; enforces 4-byte alignment on uncompressed entries. |
| `PackagingTask.kt` | Orchestrates the end-to-end build: bytecode -> DEX -> AXML -> APK -> sign -> verify. |
| **`axml/`** | **Pure Kotlin Binary Android XML Engine** |
| `AxmlWriter.kt` | Low-level binary XML chunk emitter (`RES_XML_TYPE`, `RES_STRING_POOL_TYPE`, `RES_XML_RESOURCE_MAP_TYPE`). |
| `ManifestGenerator.kt` | Higher-level binary `AndroidManifest.xml` builder emitting exact 20-byte attribute structs. |
| **`build/`** | **Build Pipeline & Directory Layout** |
| `BuildCommand.kt` | Command handler for `kui build`; coordinates compiler, packaging, and profiling. |
| `BuildDirectories.kt` | Standardized directory layout for `.kui/build/` (`classes/`, `dex/`, `apk/`, `reports/`). |
| **`cache/`** | **Incremental Build Cache** |
| `BuildCache.kt` | Key-value store mapping file content hashes to build outputs to bypass redundant work. |
| `FileHasher.kt` | High-speed SHA-256 incremental hashing utility for sources and assets. |
| **`classfile/`** | **Pure JVM Bytecode Parser** |
| `ClassFileReader.kt` | Reads compiled JVM `.class` binaries, parsing headers, constant pool, fields, and methods. |
| `ClassFile.kt` | In-memory model representing parsed class structures and bytecode attributes. |
| **`cli/`** | **Command-Line Interface** |
| `Main.kt` | CLI entry point (`kui`); parses arguments and routes to subcommands. |
| `CliDispatcher.kt` | Routes commands (`new`, `build`, `run`, `test`, `clean`, `devices`, `doctor`). |
| **`compiler/`** | **Kotlin Compiler Driver** |
| `CompileTask.kt` | Manages source scanning, arguments assembly, and standalone `kotlinc` execution. |
| `CompilerBenchmark.kt` | Micro-benchmarking utilities measuring compiler latency and throughput. |
| **`config/`** | **Configuration Parsing** |
| `KuiConfig.kt` | Type-safe data structures for `kui.toml` (`[project]`, `[android]`, `[build]`). |
| `ConfigParser.kt` | Pure Kotlin TOML parser extracting keys, integers, tables, and lists. |
| **`device/`** | **ADB Integration & Device Execution** |
| `DeviceManager.kt` | Discovers ADB devices, selects target, executes `install -r -d -t`, and launches activity. |
| **`dex/`** | **Pure Kotlin Dalvik Executable (DEX) Compiler** |
| `ClassToDexCompiler.kt` | Translates JVM `.class` bytecode into Dalvik bytecode, allocates registers, enforces constructor verification. |
| `DexModel.kt` | Binary DEX emitter: string table, type table, proto table, method table, instruction fixups, Modified UTF-8 (`encodeMutf8`), Adler-32/SHA-1 checksums, and `map_list`. |
| **`doctor/`** | **Environment Diagnostics** |
| `DoctorCommand.kt` | Validates host environment (JDK 21, `kotlinc`, `adb`, path configurations). |
| **`profiling/`** | **Build & Phase Profiling** |
| `BuildProfiler.kt` | Tracks phase durations (parsing, compiling, dexing, signing) and prints performance summaries. |
| **`project/`** | **Project Scaffolding** |
| `ProjectGenerator.kt` | Generates new projects for `kui new <name>` with template sources, tests, assets, and config. |
| **`signing/`** | **Cryptographic APK v2 Signer** |
| `ApkV2Signer.kt` | Pure Kotlin APK Signature Scheme v2 implementation; generates RSA keys, hashes 1MB chunks, injects block. |
| `ApkV2Verifier.kt` | Cryptographically verifies signed APKs to detect tampering and ensure installation compatibility. |
| **`testing/`** | **Platform Test Runner** |
| `TestRunner.kt` | Discovers and executes project test cases (`kui test`). |

---

## 🎨 UI4 Declarative Engine: `platform/ui4/`

| Directory / File | Responsibilities & Description |
| :--- | :--- |
| **`api/`** | **DSL Application Entry & Hierarchy** |
| `UI4.kt` | Primary declarative DSL builders: `app`, `screen`, `column`, `row`, `box`, `center`, `text`, `button`, `textField`, etc. |
| **`core/`** | **Geometric Primitives & Constraints** |
| `Geometry.kt` | `Point`, `Size`, and `Rect` 2D floating-point geometric primitives. |
| `Constraints.kt` | Min/max width and height constraints governing the 2-pass layout system. |
| `Alignment.kt` | Positioning alignments (`TopStart`, `Center`, `BottomEnd`, etc.). |
| `Color.kt` | RGBA 32-bit color abstraction with hex parsing and color manipulation. |
| `NodeId.kt` | Unique 64-bit identifier assigned to every node in the UI tree. |
| `ScrollState.kt` | Maintains scroll offsets and viewport scroll positions. |
| **`tree/`** | **UI Hierarchy & Dirty Tracking** |
| `UiNode.kt` | Base abstract node class defining measure, layout, render, and hit-testing contracts. |
| `UiRoot.kt` | Root node of the entire UI tree; owns the layout pipeline and dirty propagation. |
| `DirtyTracking.kt` | Tracks invalidated nodes so only dirty subtrees recalculate layout and repaint. |
| `Traversal.kt` | Depth-first and breadth-first tree traversal iterators. |
| `TreeOperations.kt` | Node insertion, replacement, removal, and reparenting utilities. |
| `DebugTree.kt` | Formats the in-memory UI hierarchy into an ASCII tree string for debugging. |
| **`layout/`** | **Concrete Node Implementations** |
| `BoxNode.kt` | Multi-child container with background color, padding, and alignment. |
| `ColumnNode.kt` | Vertical linear layout with configurable spacing/gap and cross-axis alignment. |
| `RowNode.kt` | Horizontal linear layout with configurable spacing/gap and cross-axis alignment. |
| `CenterNode.kt` | Centers a single child within available parent constraints. |
| `StackNode.kt` | Overlays multiple children on top of each other with z-ordering. |
| `SpacerNode.kt` | Flexible or fixed-size empty spacing element. |
| `TextNode.kt` | Displays formatted text with word wrapping, font size, color, and weight. |
| `ButtonNode.kt` | Clickable interactive surface with hover, pressed, and disabled states. |
| `TextFieldNode.kt` | Single-line or multi-line text input field with cursor tracking and key handling. |
| **`render/`** | **Rendering Pipeline & Recording Canvas** |
| `UiCanvas.kt` | Abstract canvas drawing interface (`drawRect`, `drawText`, `clipRect`, `save`, `restore`). |
| `RecordingCanvas.kt` | In-memory display list recorder storing draw commands for headless testing and verification. |
| `RenderPipeline.kt` | Executes render passes over clean layout trees and tracks rendering metrics. |
| `RenderMetrics.kt` | Frame timing, drawn command count, and dirty node statistics. |
| **`state/`** | **Reactive State Management** |
| `State.kt` | Read-only observable state interface (`State<T>`). |
| `MutableState.kt` | Writable reactive state (`mutableStateOf<T>`) that marks owner nodes dirty upon modification. |
| `StateBinding.kt` | Two-way and one-way binding helpers linking state to UI properties. |
| **`input/`** | **Event Handling & Focus Management** |
| `InputManager.kt` | Dispatches pointer (mouse/touch) and keyboard events to hit-tested nodes. |
| `FocusManager.kt` | Manages active keyboard focus, tab traversal, and focus loss. |
| `PointerEvent.kt` | Pointer down, move, up, cancel event representations. |
| `KeyEvent.kt` | Keyboard key-down, key-up, and char input representations. |
| **`gesture/`** | **High-Level Gesture Recognizers** |
| `GestureRecognizer.kt` | Recognizes taps, double taps, long presses, drags, and flings from raw pointer streams. |
| `Gestures.kt` | DSL modifiers (`onClick`, `onLongClick`, `onDrag`) for attaching gestures to nodes. |
| **`animation/`** | **Interpolation & Frame Clocks** |
| `Animation.kt` | Value animations (`animateFloat`, `animateColor`), duration, and repeat modes. |
| `Easing.kt` | Standard easing functions (`Linear`, `EaseIn`, `EaseOut`, `EaseInOut`, `Cubic`). |
| **`text/`** | **Typography & Font Metrics** |
| `Typography.kt` | Standard text styles (`Headline`, `Title`, `Body`, `Label`, `Caption`). |
| `TextStyle.kt` | Text properties: fontSize, color, fontWeight, letterSpacing, lineHeight. |
| **`accessibility/`** | **Semantics & Accessibility Tree** |
| `Semantics.kt` | Semantic labels, roles (`Button`, `Header`, `Input`), and screen-reader accessibility traits. |
| **`platform/android/`** | **Android Host Surfaces** |
| `AndroidHost.kt` | Surface contracts: `UiHost`, `VirtualHost` (headless tester), and `AndroidHostBridge` (View onDraw/onTouch bridge). |

---

## 🧪 Tests & Verification: `tests/`

- `tests/RepositoryBootstrapTest.kt`: Validates repository bootstrap, config parsing, and directory setup (Milestone A).
- `tests/CompilerDriverTest.kt`: Validates Kotlinc driver, classfile loading, and compilation tasks (Milestone B).
- `tests/ProjectConfigTest.kt`: Validates TOML project configuration, incremental caching, and hashing (Milestone C).
- `tests/UI4CoreAndLayoutTest.kt`: Validates UI4 node tree, 2-pass layout, and layout caching (Milestone D).
- `tests/UI4RenderStateAndInputTest.kt`: Validates state reactivity, dirty tracking, rendering, and gestures (Milestone E).
- `tests/UI4MilestoneFTest.kt`: Validates VirtualHost, focus management, semantics, and input dispatch (Milestone F).
- `tests/ToolchainAndApkTest.kt`: Validates pure Kotlin DEX compiler, AXML writer, APK packaging, APK v2 signing, and ADB execution (Milestone G - 78 tests).

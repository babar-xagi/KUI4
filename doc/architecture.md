# 🏗️ Architecture

```text
┌──────────────────────────────────────────┐
│             Developer App                │
│                 Kotlin                   │
└───────────────────┬──────────────────────┘
                    ▼
┌──────────────────────────────────────────┐
│                   UI4                    │
│ API • State • Layout • Input • Nav       │
│ Accessibility • Theme • Services         │
└───────────────────┬──────────────────────┘
                    ▼
┌──────────────────────────────────────────┐
│              UI4 Runtime                 │
│ UI Tree • Dirty Tracking • Render Tree   │
│ Text • Gestures • Animation              │
└───────────────────┬──────────────────────┘
                    ▼
┌──────────────────────────────────────────┐
│                   KUI                    │
│ Project • Build • Compiler • Cache       │
│ DEX • Resources • APK • Signing          │
│ Device • Test • Profile                  │
└───────────────────┬──────────────────────┘
                    ▼
               Android / ART
```

## Subsystem Responsibilities

### UI4 Platform
1. **API**: Declarative DSL (`screen`, `column`, `row`, `box`, `text`, `button`).
2. **Tree**: Lightweight node hierarchy avoiding `android.view.View` allocations.
3. **State**: Reactive value holders with fine-grained subscription tracking.
4. **Layout**: Two-phase intrinsic & constraint-driven measurement pass.
5. **Render**: Hardware-accelerated canvas draw commands.
6. **Input & Gestures**: Pointer event capture, hit testing, drag, fling, and tap recognizers.
7. **Navigation**: Pure state stack and transition coordination.
8. **Accessibility**: Semantics node mapping to Android AccessibilityNodeInfo.

### KUI Toolchain
1. **CLI**: Ergonomic command-line runner (`kui new`, `kui run`, `kui build`, `kui test`).
2. **Project Model**: Lightweight `kui.toml` parser with strict validation.
3. **Build Graph**: Directed acyclic task graph with content-addressable caching.
4. **Compiler Invoker**: Direct invocation of official `kotlinc`.
5. **DEX Pipeline**: Pure Kotlin bytecode to Dalvik Executable (DEX) compiler.
6. **AXML & Packaging**: Pure Kotlin binary XML encoder and uncompressed/compressed ZIP/APK writer.
7. **APK Signing**: V2 / V3 signature block generator with deterministic key generation.
8. **Device Bridge**: ADB integration for streaming install and fast activity launch.

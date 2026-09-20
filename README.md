# 🟣 UI4 + KUI — Pure Kotlin Application Platform

> **North Star:** Build a complete Kotlin-first application platform where developers create modern declarative UI apps with a Compose-like experience, but without Gradle, AGP, Jetpack Compose, Material3, AndroidX UI runtime, or external build orchestration. The developer needs only the official Kotlin compiler/JDK foundation plus UI4/KUI, whose own tooling is written in Kotlin.

---

## 🌍 Overview

- **UI4**: The modern minimal declarative UI framework and runtime (API, UI tree, layout, rendering, state, events, gestures, text, animation, navigation, accessibility).
- **KUI**: The developer toolchain and CLI (`kui new`, `kui run`, `kui build`, `kui test`, `kui doctor`) owning project loading, compilation, DEX compilation, resource packaging, APK signing, and device deployment.

```text
Developer writes Kotlin
        ↓
UI4 declarative UI
        ↓
KUI project / build system
        ↓
Official Kotlin compiler
        ↓
KUI-owned DEX / resources / APK / signing
        ↓
Install + launch
        ↓
Native Android application
```

---

## 📁 Repository Structure

```text
KUI4/
├── kui.toml                 # Root platform configuration
├── README.md                # Platform overview and quickstart
├── LICENSE                  # Apache 2.0 license
├── UI4_KUI_PURE_KOTLIN_...  # Master 180-phase roadmap
├── docs/                    # Architecture, vision, specifications
│   ├── vision.md
│   ├── architecture.md
│   ├── ui-api.md
│   ├── project-format.md
│   ├── compiler.md
│   ├── dex.md
│   ├── packaging.md
│   ├── accessibility.md
│   ├── performance.md
│   ├── testing.md
│   ├── decisions/
│   └── reports/
├── platform/
│   ├── ui4/                 # UI4 UI framework & runtime
│   │   ├── api/             # Developer-facing DSL (screen, text, button, column)
│   │   ├── core/            # Primitives, lifecycle, geometry
│   │   ├── tree/            # Lightweight UI node hierarchy
│   │   ├── state/           # Reactive state & signals
│   │   ├── layout/          # Measure & layout algorithms
│   │   ├── render/          # Canvas & graphics layer
│   │   ├── text/            # Font rendering & typography
│   │   ├── input/           # Pointer input & touch dispatch
│   │   ├── gesture/         # Tap, scroll, drag, fling
│   │   ├── animation/       # Easing curves, transitions, springs
│   │   ├── navigation/      # State-driven declarative backstack
│   │   ├── accessibility/   # Semantics tree & screen reader bridge
│   │   ├── theme/           # Design system tokens & colors
│   │   ├── resources/       # Asset & string resolution
│   │   └── platform/android/# Native Android Surface/Canvas integration
│   └── kui/                 # KUI toolchain & CLI
│       ├── cli/             # Entry point & command routing
│       ├── project/         # Project model & kui.toml parser
│       ├── config/          # Diagnostic validation & defaults
│       ├── build/           # Task graph & build orchestrator
│       ├── cache/           # Content-addressable build cache
│       ├── compiler/        # kotlinc invoker & options
│       ├── classfile/       # JVM bytecode reader & analyzer
│       ├── dex/             # DEX file format encoder
│       ├── axml/            # Binary Android XML encoder
│       ├── resources/       # Resource table generator (arsc)
│       ├── apk/             # ZIP / APK package builder
│       ├── signing/         # APK Signature Scheme v2/v3
│       ├── device/          # ADB bridge, install & launch
│       ├── testing/         # Test runner & reporting
│       └── profiling/       # Benchmark harness & metrics
├── examples/                # Reference UI4 applications
│   └── hello/               # Baseline starter app
├── tests/                   # Monorepo unit & integration tests
├── benchmarks/              # Performance & startup time benchmarks
└── scripts/                 # Bootstrap & verification utilities
```

---

## ⚡ Zero-Dependency Toolchain Guarantee

KUI4 requires **no internet access** and **no external package downloads** to build and run. The entire toolchain relies exclusively on:
1. Official Kotlin Compiler (`kotlinc`)
2. Standard JDK (Java 21)
3. Local Android SDK platform tools (`adb` / `android.jar`)
4. Pure Kotlin code inside UI4 and KUI.
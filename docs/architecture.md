# 🏗️ Architecture Overview

KUI is the Kotlin build toolchain. UI4 is the Kotlin UI framework.

## Build flow

```text
Kotlin application + UI4 API
          ↓ kotlinc
      JVM class files
          ↓ KUI DEX + manifest writers
    DEX + binary Android manifest
          ↓ aligned ZIP + APK v2 signing
       signed APK
          ↓ optional ADB install and launch
       Android device
```

The current DEX translator has limited instruction support. For simple applications it synthesizes a native text Activity from literal UI source content; it does not execute the full UI4 tree.

## Framework flow

On the JVM, the DSL constructs nodes; layout calculates sizes and positions; explicit state bindings update nodes; input and rendering use framework abstractions. `VirtualHost` and `RecordingCanvas` support headless verification.

A complete Android Canvas adapter, UI lifecycle, and OS input/accessibility integration remain in development.

KUI signs APKs with v2 RSA signatures. V3 signing, automatic recomposition, complete resource-table generation, and device profiling are not current capabilities.

[Build architecture](developer/architecture.md) · [Runtime architecture](developer/ui4-engine.md)
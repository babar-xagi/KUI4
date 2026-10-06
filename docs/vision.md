# 🌍 Project Vision

KUI aims to let developers create Android applications with a small Kotlin project and a readable declarative UI.

**KUI** owns the developer toolchain. **UI4** owns the UI framework.

The desired everyday workflow is:

```powershell
kui new hello
cd hello
kui run
```

The current build pipeline avoids a full Android SDK and implements DEX, binary manifests, aligned APKs, and v2 signing in Kotlin. Application builders still need Kotlin and a JDK; device deployment uses ADB.

The longer-term goal is to run the same UI4 tree, state, rendering, and input behavior on a device that developers validate in JVM tests. That requires completing general bytecode/runtime support and Android hosting.

See the [current capability table](../README.md#-what-works-today), [developer experience proposal](developer/developer-experience.md), and [platform roadmap](../UI4_KUI_PURE_KOTLIN_PLATFORM_ROADMAP.md).
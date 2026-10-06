# 🟣 KUI — Kotlin UI Platform

**A Kotlin toolchain and declarative UI framework for Android development experiments.**

Write UI in Kotlin, build a signed APK, and deploy it with a small command-line workflow.

[Getting started](docs/user/getting-started.md) · [Learn UI4](docs/user/learning-path.md) · [CLI reference](docs/user/cli-reference.md) · [Developer guide](docs/developer/README.md)

> **Project status: experimental.** KUI builds APKs with a generated native text Activity. The complete interactive UI4 runtime is not yet connected to Android, and the DEX translator does not preserve arbitrary Kotlin application logic.

## ✨ Meet KUI and UI4

| Component | Responsibility |
| --- | --- |
| **KUI** | Project creation, Kotlin compilation, DEX generation, APK packaging and signing, JVM tests, and ADB deployment |
| **UI4** | Declarative UI, layout, observable state, input, gestures, rendering abstractions, and navigation models |

Both implementations are written in Kotlin. Windows launchers provide the command-line entry point.

## 🚀 Quick start

Install the tools below, then open PowerShell in the repository directory.

```powershell
.\kui.bat doctor
.\kui.bat new hello
cd hello
..\kui.bat build
```

Your APK is generated at:

```text
build/outputs/apk/debug/app-debug.apk
```

For a connected Android phone with USB debugging enabled:

```powershell
adb devices
..\kui.bat run
```

Add the KUI directory to PATH if you prefer `kui build` and `kui run` from any project. The [setup guide](docs/user/getting-started.md) explains both workflows.

## 🎨 A UI you can read

The smallest UI4 application needs an application, a screen, and some content:

```kotlin
import ui4.*

fun main() = app {
    screen {
        text("Hello, KUI!")
    }
}
```

Use layouts when you need more structure:

```kotlin
import ui4.*

fun main() = app {
    screen(backgroundColor = Color.hex("#121212")) {
        column(gap = 12) {
            text("Welcome", style = TextStyle.Headline)
            text("Your UI is written in Kotlin.")
        }
    }
}
```

The UI4 tree supports these layouts on the JVM. The current Android backend extracts literal text and supported literal styling into one TextView; it does not yet reproduce the whole UI4 layout tree.

Start with [five UI4 concepts](docs/user/learning-path.md), then explore [components and styling](docs/user/ui-components-and-styling.md).

## 🧰 Requirements

These requirements apply to the computer building applications.

| Tool | Requirement | Purpose |
| --- | --- | --- |
| Kotlin compiler | 2.0+; verified with 2.4.20 | Compile Kotlin sources |
| Java JDK | 21+; verified with JDK 25 | Run KUI and the JVM Kotlin compiler |
| ADB platform-tools | Optional for builds | Install and launch on Android devices |

Building does not require Android Studio, Gradle, AGP, Android SDK platforms, SDK build-tools, AAPT2, D8/R8, zipalign, or apksigner.

Java is still required. People installing a built APK on their phones do not need Kotlin, a JDK, or development tools.

**Host support:** the supplied launchers and release scripts support Windows. A packaged Linux/macOS launcher is not currently included.

## 📁 A small project structure

```text
hello/
├── kui.toml          # Application configuration
├── src/main.kt       # UI4 application
├── tests/AppTest.kt  # Starter JVM test program
├── assets/           # Files packaged into the APK
├── .gitignore
└── README.md
```

KUI generates this structure for you. See the [project guide](docs/user/project-guide.md) for configuration and output paths.

## 📱 What works today

| Area | Current capability |
| --- | --- |
| Project workflow | Create, inspect, compile, clean, and test projects |
| APK pipeline | Kotlin DEX writer, binary manifest, aligned ZIP, APK v2 signing and verification |
| Android output | Generated text Activity with literal text, supported background colors, contrast, and alignment |
| Device commands | Discover, install, and launch through ADB |
| UI4 framework | JVM-tested layout, state bindings, input, gestures, rendering, and navigation foundations |
| Still in development | General JVM-to-DEX translation and full interactive Android hosting |
| Not implemented | Device profiling, JUnit discovery, and Android image/font rendering through UI4 |

The framework API and Android output have different capabilities today. Each tutorial identifies which behavior it demonstrates.

## 🧪 Verify the platform

At the repository root:

```powershell
.\kui.bat test
```

For individual suite logs:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\test_all.ps1
```

The last full verification on **2026-10-05** passed **18 test programs and 575 checks**. It covered compiler integration, UI4 behavior, DEX structures, APK alignment, signatures, and tamper detection. Phone deployment remained pending when the device disconnected; these results do not claim complete Android runtime coverage.

Learn how to add checks in the [testing guide](docs/testing.md).

## 📦 Build a Windows distribution

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\build_distribution.ps1
```

Output for release **0.04**: `dist/kui-0.4.0-windows.zip` and its SHA-256 checksum. The CLI and installer use version `0.4.0`; the release tag is `v0.04`. The package includes a precompiled KUI JAR; application builds still require Kotlin and a JDK.

MSI creation is available to maintainers with WiX installed:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\build_msi.ps1
```

See [distribution notes](RELEASES.md). The Kotlin migration's MSI build has not been verified locally because WiX was unavailable.

## 📚 Documentation

| Start here | Go deeper |
| --- | --- |
| [Getting started](docs/user/getting-started.md) | [Architecture](docs/developer/architecture.md) |
| [Learn UI4](docs/user/learning-path.md) | [Compiler and packaging](docs/developer/toolchain-and-compiler.md) |
| [Working examples](docs/user/cookbook-and-examples.md) | [UI4 runtime](docs/developer/ui4-engine.md) |
| [Troubleshooting](docs/user/troubleshooting.md) | [Contributing](docs/developer/contributing.md) |

For the next steps toward a simpler API and workflow, read the [developer experience proposal](docs/developer/developer-experience.md). Those proposed features are not implemented yet.

## 🤝 Contributing and license

Contributions to the Kotlin toolchain, Android runtime, tests, and documentation are welcome. Start with the [contributor guide](docs/developer/contributing.md).

KUI is licensed under [Apache License 2.0](LICENSE). See [CHANGELOG.md](CHANGELOG.md) for project history.

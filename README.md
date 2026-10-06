# KUI: Kotlin UI platform

KUI and the UI4 framework are implemented in Kotlin. The CLI compiles application sources with the standalone Kotlin compiler, writes DEX and binary Android manifests, packages and signs APKs, and uses ADB to install and launch them.

## Requirements

- Kotlin compiler 2.0 or newer (tested with 2.4.20).
- JDK 21 or newer (tested with JDK 25).
- ADB platform-tools only when installing or launching on a phone.

Building APKs does not require Android Studio, Gradle, Android SDK platforms, SDK build-tools, AAPT2, D8, zipalign, or apksigner. Java remains necessary because the Kotlin compiler and KUI run on the JVM.

## Windows quick start

From this repository:

```powershell
.\kui.bat doctor
.\kui.bat new myapp
cd myapp
..\kui.bat test
..\kui.bat run
```

The first invocation compiles the Kotlin platform into `.kui/build/kui.jar`. Subsequent invocations reuse it until platform sources or compiler inputs change. Both Windows launchers always run the Kotlin CLI. Add the repository directory to PATH to use `kui` from other directories.

```kotlin
import ui4.*

fun main() = app {
    screen {
        center {
            text("Hello from Kotlin!")
        }
    }
}
```

APKs are written to `build/outputs/apk/debug/app-debug.apk` inside the application project.

## Tests and distribution

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\test_all.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\build_distribution.ps1
```

The test runner executes every Kotlin suite, including compiler integration, UI4 layout/input/state, DEX, APK alignment, signatures, and tamper detection. Logs are stored in `.kui/verification`. Existing milestone scripts also use this runner.

Running `kui.bat test` at the repository root also executes all repository test programs through the Kotlin runner. Inside an application project, it executes that project's tests.

The portable Windows ZIP contains a precompiled Kotlin JAR and launchers. Users still need Kotlin and a JDK. MSI creation additionally requires WiX on the maintainer's machine:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\build_msi.ps1
```

## Current scope

The Android backend is experimental. It synthesizes a native Activity displaying extracted literal UI text. The class-to-DEX translator does not yet preserve arbitrary Kotlin application logic. Interactive UI4 state, gestures, layouts, and rendering are tested on the JVM; the complete engine is not wired into the Android Activity. Device profiling is not implemented.

[User guide](docs/user/README.md) · [Developer guide](docs/developer/README.md) · [Roadmap](UI4_KUI_PURE_KOTLIN_PLATFORM_ROADMAP.md)

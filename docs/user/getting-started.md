# 🚀 Getting Started on Windows

This guide takes you from the source repository to your first signed APK.

## 1. Prepare the build tools

Install Kotlin 2.0+ and JDK 21+. KUI has been verified with Kotlin 2.4.20 and JDK 25.

Check them in PowerShell:

```powershell
kotlinc -version
java -version
where.exe kotlinc
where.exe java
```

Use `where.exe`; PowerShell's `where` alias does not locate executables.

Place both bin directories on PATH, or set `KOTLIN_HOME` and `JAVA_HOME`. The launcher can also discover common Windows installations. Newly installed tools may require reopening the terminal.

## 2. Open the repository

If you use Git:

```powershell
git clone https://github.com/babar-xagi/KUI4.git
cd KUI4
.\kui.bat doctor
```

Alternatively, extract a source archive and open PowerShell in its root directory.

The first source invocation compiles the Kotlin platform into `.kui/build/kui.jar`. Later invocations reuse it until compiler inputs or platform sources change. Portable distributions include `lib/kui.jar`.

## 3. Create and build an application

```powershell
.\kui.bat new myapp
cd myapp
..\kui.bat build
```

Open `src/main.kt` and try this complete example:

```kotlin
import ui4.*

fun main() = app {
    screen {
        text("Hello from my first KUI app!")
    }
}
```

Build again after editing. The signed APK is written to `build/outputs/apk/debug/app-debug.apk`.

This greeting fits the current generated Android text Activity. More complex UI4 layouts and interaction currently remain JVM framework capabilities.

## 4. Add a real test

The generated `tests/AppTest.kt` is a starter placeholder. Replace its contents with actual checks, for example:

```kotlin
package com.example.myapp

fun main() {
    val greeting = "Hello from my first KUI app!"
    check(greeting.isNotBlank())
    println("Greeting test passed")
}
```

Run:

```powershell
..\kui.bat test
```

Tests run on the JVM. See the [testing guide](../testing.md) for UI state and tree checks.

## 5. Run on a phone

Install standalone Android platform-tools and add their directory to PATH. Enable USB debugging, connect your phone, and authorize the computer.

```powershell
adb devices
..\kui.bat run
```

KUI builds, installs, and launches the application on the first online device. If no device is connected, `run` leaves the built APK ready and prints connection guidance; it does not prove a successful launch.

## 💡 Use shorter commands

After adding the KUI directory to your user PATH and opening a new terminal, you can use:

```powershell
kui new anotherapp
cd anotherapp
kui build
kui test
kui run
```

Only app developers need Kotlin and Java on their computers. People installing your APK on Android need neither.

**Next:** [Learn UI4](learning-path.md) · [Project guide](project-guide.md) · [Troubleshooting](troubleshooting.md)
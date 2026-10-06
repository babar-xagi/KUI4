# Troubleshooting

Run `kui doctor` first. Kotlin must be 2.0+ and Java must be 21+. Use `where.exe java` and `where.exe kotlinc` in PowerShell; `where` is a PowerShell alias and does not locate executables.

If tools were just installed, restart the terminal or set JAVA_HOME/KOTLIN_HOME. The KUI launcher updates only its process environment.

If no phone is available, APK building still works. For deployment, put ADB platform-tools on PATH, enable USB debugging, authorize the computer, and check `adb devices`. An unauthorized or offline device cannot run the app.

Compilation errors stop the build. Use `kui build --clean` to regenerate application outputs. The platform launcher rebuilds its own cached JAR when sources change.

A successful build does not guarantee support for arbitrary Kotlin code on Android. The experimental DEX translator and Android host currently support the generated text Activity. Interactive UI4 logic remains a JVM framework capability pending Android integration.

Project tests require top-level main functions in tests/. A thrown exception or nonzero exit fails `kui test`. Device profiling is not available; `kui bench` runs JVM benchmarks.
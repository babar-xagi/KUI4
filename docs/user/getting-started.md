# Getting started on Windows

Install Kotlin 2.0+ and a JDK 21 or newer. Add their bin directories to PATH, or set KOTLIN_HOME and JAVA_HOME. The launcher also discovers common Windows installation locations.

From the KUI repository:

```powershell
.\kui.bat doctor
.\kui.bat new myapp
cd myapp
..\kui.bat test
..\kui.bat build
```

The signed APK is `build/outputs/apk/debug/app-debug.apk`.

To run it on a phone, install standalone Android platform-tools and add its directory to PATH. Enable USB debugging on the phone, connect it, and authorize the computer:

```powershell
adb devices
..\kui.bat run
```

The first repository invocation compiles KUI; later invocations reuse the platform JAR. Packaged distributions include a precompiled JAR. Kotlin and Java are still required for application compilation.
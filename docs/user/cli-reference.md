# Kotlin CLI reference

Run `kui.bat` from this repository, or add the repository directory to PATH and use `kui`.

| Command | Behavior |
| --- | --- |
| `new <name>` | Create an application project |
| `doctor` | Validate Kotlin and JDK 21+; report optional ADB |
| `info` | Read project metadata |
| `build [--clean] [--verbose]` | Compile Kotlin and create a signed APK |
| `run` | Build, install, and launch using ADB |
| `install` | Build and install using ADB |
| `launch` | Launch the installed application |
| `devices` | List connected ADB devices |
| `test` | Compile src/ and tests/ and execute test main functions on the JVM |
| `clean` | Remove intermediate build and compiler cache directories |
| `bench [compiler]` | Run JVM foundation or compiler benchmarks |
| `ui-tree` | Print a demonstration UI4 tree |
| `profile` | Not implemented; returns failure |
| `version` / `--version` | Print the Kotlin platform version |
| `help [command]` / `--help` | Show usage |

Tests must define top-level `fun main()` or `fun main(args: Array<String>)`. Exceptions, failed assertions, compilation errors, and nonzero exits cause `kui test` to fail. JUnit annotation discovery is not implemented.

Application class files live in `.kui/build/classes`; APKs live in `build/outputs/apk/debug/app-debug.apk`. APK building uses Kotlin and Java without Android SDK build-tools. ADB platform-tools are required for device commands.

The generated Android backend currently presents extracted literal text, background colors, and alignment in a native Activity. Arbitrary Kotlin application logic and complete interactive UI4 Android hosting are not yet supported.
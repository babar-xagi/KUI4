# 💻 CLI Reference

The examples below use `kui` on PATH. From the repository root, use `.\kui.bat`; from a directly nested application folder, use `..\kui.bat`.

## Commands

| Command | Behavior |
| --- | --- |
| `new <name>` | Generate an application directory |
| `doctor` | Check Kotlin, JDK 21+, and optional ADB availability |
| `info` | Print project metadata |
| `build` | Compile Kotlin and package a signed APK |
| `run` | Build, install, and launch on the first online ADB device |
| `install` | Install the current APK; build first if no APK exists |
| `launch` | Launch the installed application's MainActivity |
| `devices` | List connected ADB devices |
| `test` | Compile and execute project JVM test programs; at the repository root, execute repository tests |
| `clean` | Remove `.kui/build` and `.kui/cache`; retain the final APK |
| `bench` | Run foundation benchmarks |
| `bench compiler` | Run compiler benchmarks |
| `ui-tree` | Print a demonstration UI4 tree |
| `profile` | Not implemented; return a failure exit code |
| `version` / `--version` | Print the platform version |
| `help [command]` / `--help` | Show usage |

## 🚀 Create a project

```powershell
kui new notes --app-id=com.example.notes
```

| Option | Purpose |
| --- | --- |
| `--app-id=<id>` | Application package ID; `--application-id` is an alias |
| `--minimal` | Omit starter tests, assets, and generated .gitignore |
| `--version=<value>` | Initial project version name |
| `--version-code=<number>` | Recorded generator metadata; APK version code currently remains 1 |
| `--min-sdk=<number>` | Initial minimum API value; default 24 |
| `--target-sdk=<number>` | Initial target API value; default 36 |

A name starts with a letter and contains letters, numbers, dashes, or underscores.

## 📦 Build and deploy

```powershell
kui build --clean
kui build --verbose
kui run
```

`--clean` regenerates intermediate files and caches before compiling. Use it after changing assets. `--verbose` prints compiler details.

`install` can reuse an existing APK. Build explicitly after source changes before using it.

If no phone is connected, `run` can still build successfully and print connection instructions. Successful packaging alone is not evidence that an application was installed or launched.

## 🧪 Tests

Test files must define a top-level `fun main()` or `fun main(args: Array<String>)`. Exceptions, failed checks/assertions, compilation errors, and nonzero exit codes fail `kui test`. The runner does not discover JUnit annotations.

[Project guide](project-guide.md) · [Testing details](../testing.md) · [Troubleshooting](troubleshooting.md)
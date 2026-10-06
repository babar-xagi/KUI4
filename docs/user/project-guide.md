# 📁 Project Guide

KUI projects keep application code, tests, assets, and configuration in separate directories.

## Generated files

```text
myapp/
├── kui.toml
├── src/main.kt
├── tests/AppTest.kt
├── assets/
│   ├── images/
│   └── fonts/
├── .gitignore
└── README.md
```

`kui new myapp --minimal` omits the tests, assets, and generated .gitignore. Use the standard template while learning.

The starter test prints a placeholder success message. Add meaningful checks before relying on it.

## ⚙️ Application configuration

```toml
[project]
name = "myapp"
version = "0.1.0"
application_id = "com.example.myapp"

[android]
min_sdk = 24
target_sdk = 36

[ui]
theme = "system"
```

| Key | Behavior |
| --- | --- |
| `project.name` | Required; used as the application label |
| `project.version` | Required; used as the version name |
| `project.application_id` | Android package ID; generated projects receive an explicit value |
| `project.description` | Optional project metadata |
| `android.min_sdk` | Defaults to 24; recommended baseline for current APK v2 output |
| `android.target_sdk` | Defaults to 36; written to the manifest, not an SDK download requirement |
| `ui.theme` | Parsed metadata; automatic Android theme switching is not implemented |

The generator also records `version_code` and `generator_version`. The current packager writes manifest version code 1; changing the recorded `version_code` does not yet change APK versioning.

## 💻 Source code

All Kotlin files under `src/` are discovered recursively. Start with `src/main.kt`; split code into packages as the application grows.

`app { ... }` constructs a UI4 tree. The current Android backend synthesizes its own Activity rather than invoking this Kotlin main with the full UI4 runtime.

## 🎨 Assets

Files in `assets/` are copied into the APK under `assets/`. Packaging an image or font does not make it render automatically. The public UI4 DSL does not currently provide an `image(asset = ...)` widget.

After changing assets, use `kui build --clean` to ensure they are repackaged. The current fast cache check does not include asset content.

## 📦 Generated outputs

```text
.kui/
├── build/
│   ├── classes/       # Application JVM classes
│   └── ui4-api.jar    # Compiled UI4 API
├── cache/             # Compiler cache
└── tests/             # Compiled JVM tests

build/
└── outputs/apk/debug/app-debug.apk
```

DEX and the binary manifest are assembled during packaging; separate `build/intermediates/dex/` files are not currently emitted by the application build.

`kui clean` removes `.kui/build` and `.kui/cache`. It does not delete the final APK or the `.kui/tests` directory.

Keep generated outputs out of version control, including your application's `build/` directory.

[CLI reference](cli-reference.md) · [Project format](../project-format.md)
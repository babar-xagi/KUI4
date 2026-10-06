# 📄 Project Format

A standard project has `kui.toml`, `src/`, `tests/`, `assets/`, a README, and a generated .gitignore.

```toml
[project]
name = "hello"
version = "0.1.0"
application_id = "com.example.hello"

[android]
min_sdk = 24
target_sdk = 36

[ui]
theme = "system"
```

Required identity fields are `project.name` and `project.version`. The Android API defaults are 24 and 36. These numbers control manifest declarations; they do not require downloading SDK platforms.

The generator records `version_code` and `generator_version`; the APK packager currently writes version code 1. Theme is parsed metadata, not a complete runtime theme implementation.

Kotlin sources are discovered recursively under `src/`. Assets are copied into the APK. Compilation and test outputs live under `.kui/`; the final APK lives under `build/outputs/apk/debug/`.

See the [project guide](user/project-guide.md) for supported fields, output paths, and current limitations.
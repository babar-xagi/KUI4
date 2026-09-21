# 🌍 Vision

**UI4** is the UI/runtime framework.  
**KUI** is the developer toolchain and CLI.

```text
Developer writes Kotlin
        ↓
UI4 declarative UI
        ↓
KUI project/build system
        ↓
Kotlin compiler
        ↓
KUI-owned DEX/resources/APK/signing
        ↓
install + launch
        ↓
native Android application
```

## Desired Developer Workflow

```powershell
kui new hello
cd hello
kui run
```

Generated project:

```text
hello/
├── kui.toml
├── src/
│   └── main.kt
├── assets/
└── tests/
```

## Zero-Ceremony Guarantee

No normal UI4 project should require:
- `gradlew` / `gradle/`
- `settings.gradle.kts` / `build.gradle.kts`
- Android Gradle Plugin (AGP)
- AAPT2
- D8 / R8
- `apksigner` / `zipalign`
- External internet package dependencies during build

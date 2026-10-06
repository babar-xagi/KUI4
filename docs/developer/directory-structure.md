# Repository structure

| Path | Purpose |
| --- | --- |
| `platform/kui/cli` | Kotlin command parsing, help, and dispatch |
| `platform/kui/compiler` | Tool discovery, kotlinc execution, UI4 bootstrap, compilation cache |
| `platform/kui/classfile` | JVM class reader |
| `platform/kui/dex` | DEX models, writer, generated Android Activity |
| `platform/kui/axml` | Binary Android manifest |
| `platform/kui/apk` | Aligned ZIP and packaging pipeline |
| `platform/kui/signing` | APK v2 signing and verification |
| `platform/kui/device` | ADB discovery, install, and launch |
| `platform/kui/testing` | Project JVM test runner |
| `platform/ui4` | DSL, tree, layout, state, input, rendering, navigation |
| `tests` | Repository Kotlin verification programs |
| `examples` | Application projects |
| `scripts` | Windows bootstrap, tests, portable and MSI packaging |
| `docs` | User/developer guides and historical milestone reports |

Generated outputs: `.kui/build/kui.jar`, `.kui/verification`, project APKs under `build/outputs/apk/debug`, and release packages under `dist`.
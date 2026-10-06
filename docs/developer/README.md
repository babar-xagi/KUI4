# 🛠️ Developer Guide

KUI's implementation lives in `platform/kui`; the UI4 framework lives in `platform/ui4`. Both are Kotlin.

## Start contributing

Use Kotlin 2.0+ and JDK 21+. At the repository root:

```powershell
.\kui.bat doctor
.\kui.bat test
```

ADB is optional for JVM verification and required for device deployment. WiX is needed only for maintainers creating MSI installers.

## 🧭 Technical guides

| Guide | What it explains |
| --- | --- |
| [Architecture](architecture.md) | Build pipeline and Android runtime boundary |
| [Directory structure](directory-structure.md) | Source ownership and generated outputs |
| [Compiler and packaging](toolchain-and-compiler.md) | Class files, DEX, AXML, APK alignment, and signing |
| [DEX internals](kui-dex-internals.md) | Models, encoding, checksums, and translator limits |
| [UI4 runtime](ui4-engine.md) | Tree, layout, state bindings, rendering, and input |
| [Contributing](contributing.md) | Tests, distribution builds, and change validation |
| [Developer experience](developer-experience.md) | Proposed steps toward a simpler API and workflow |

The [user learning path](../user/learning-path.md) is the entry point for application developers. Historical milestone reports and the large platform roadmap are useful background, but are not the current support contract.
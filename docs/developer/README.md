# Kotlin developer guide

All platform implementation is Kotlin: `platform/kui` contains the CLI and APK toolchain, and `platform/ui4` contains the UI framework.

Use Kotlin 2.0+ and JDK 21+. Run `kui.bat doctor` to check tool discovery. Run `scripts/test_all.ps1` to compile and execute every repository suite. ADB is only required for testing on a connected Android device.

- [Architecture](architecture.md)
- [Directory structure](directory-structure.md)
- [Compiler and toolchain](toolchain-and-compiler.md)
- [DEX internals](kui-dex-internals.md)
- [UI4 engine](ui4-engine.md)
- [Contributing](contributing.md)
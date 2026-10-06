# 📦 Kotlin Release 0.04

Release label **0.04** uses tag **v0.04** and Kotlin/package version **0.4.0**, matching `KuiVersion` and the repository configuration. This release is experimental.

| Package | Purpose |
| --- | --- |
| `kui-0.4.0-windows.zip` | Portable Windows distribution with precompiled Kotlin JAR |
| `kui-0.4.0-windows-x64.msi` | Windows installer with PATH registration |
| `*.sha256` | SHA-256 checksums for downloaded packages |

Release assets are published through the tag-triggered GitHub Actions workflow. See [GitHub releases](https://github.com/babar-xagi/KUI4/releases).

Build a portable package with `scripts/build_distribution.ps1`; build a Windows installer with `scripts/build_msi.ps1`. Both packages contain `lib/kui.jar` and the Kotlin launchers. Checksums are generated beside the artifacts in `dist/`.

The older hybrid implementation and its checked-in binary installers have been removed. Historical release descriptions remain in [CHANGELOG.md](CHANGELOG.md) and Git history. They do not describe the current Kotlin backend.

Kotlin and JDK 21+ are required. ADB is optional for building and required for automatic device deployment. WiX is only a maintainer dependency for creating MSI files.

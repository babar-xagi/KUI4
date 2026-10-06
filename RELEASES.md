# Kotlin distribution

The current Kotlin platform version is 0.1.0, matching `KuiVersion` and the repository configuration.

Build a portable package with `scripts/build_distribution.ps1`; build a Windows installer with `scripts/build_msi.ps1`. Both packages contain `lib/kui.jar` and the Kotlin launchers. Checksums are generated beside the artifacts in `dist/`.

The older hybrid implementation and its checked-in binary installers have been removed. Historical release descriptions remain in [CHANGELOG.md](CHANGELOG.md) and Git history. They do not describe the current Kotlin backend.

Kotlin and JDK 21+ are required. ADB is optional for building and required for automatic device deployment. WiX is only a maintainer dependency for creating MSI files.
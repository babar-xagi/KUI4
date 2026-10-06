# Contributing

Install Kotlin 2.0+ and JDK 21+, then run:

```powershell
.\kui.bat doctor
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\test_all.ps1
```

The test script compiles the platform once and compiles tests against it using Kotlin friend-module access for internal framework tests. Every suite runs in a separate JVM. Failure exit codes are preserved; logs are written to `.kui/verification`.

Run a selected suite with `scripts/test_all.ps1 -Suite CompilerDriverTest`. Existing milestone wrappers select their corresponding suites.

For project tests, `kui test` compiles `src/` and `tests/`, then runs test entry points with top-level main functions. Throw an exception or exit nonzero to fail a test. This runner does not discover JUnit annotations.

At the repository root, `kui.bat test` compiles platform sources and runs all repository test programs. See the [testing guide](../testing.md) for coverage and the distinction between JVM checks and Android verification.

Build release files with `scripts/build_distribution.ps1`. MSI creation uses `scripts/build_msi.ps1` and requires WiX. Application developers do not need WiX. Distribution staging excludes generated project caches and APKs.

Keep Kotlin changes in the existing platform packages. Test actual behavior, including failures. Describe Android backend limitations accurately; JVM framework tests do not prove Android UI integration.

For documentation changes, verify local links and compile complete Kotlin examples against the current platform. Run executable example checks where applicable. Keep proposed APIs in the [developer experience proposal](developer-experience.md), separate from current API documentation.

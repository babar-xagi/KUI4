# Phase 001 — Repository bootstrap

## Goal
Create monorepo skeleton according to Section 7 of the UI4 + KUI roadmap.

## Learned
- Established clean self-contained directory taxonomy isolating the declarative UI runtime (`platform/ui4`) from the toolchain CLI and packaging engine (`platform/kui`).
- Validated offline-first standalone execution using local Kotlin compiler `kotlinc` 2.4.20 and Java 21 without Gradle, AGP, or internet access.
- Confirmed fast compile-and-execute cycle for testing tasks (<5s).

## Files Changed / Added
- `kui.toml`: Platform root configuration file
- `README.md`: Updated comprehensive documentation on UI4 + KUI architecture
- `.gitignore`: Updated with `.kui/` build and cache exclusions
- `docs/vision.md`: Core developer experience vision
- `docs/architecture.md`: Platform and toolchain block diagram
- `docs/ui-api.md`: Declarative UI specification
- `docs/project-format.md`: Standard project format definition
- `docs/compiler.md`: Kotlin compiler pipeline spec
- `docs/dex.md`: DEX compilation architecture
- `docs/packaging.md`: APK packaging and signing specification
- `docs/accessibility.md`: Semantics and accessibility model
- `docs/performance.md`: Performance and memory goals
- `docs/testing.md`: Testing tiers and philosophy
- `docs/decisions/0001-pure-kotlin-toolchain.md`: Architecture Decision Record
- `platform/ui4/*`: 15 domain subdirectories
- `platform/kui/*`: 15 toolchain subdirectories
- `examples/hello/*`: Starter project skeleton (`kui.toml`, `main.kt`, `AppTest.kt`)
- `tests/RepositoryBootstrapTest.kt`: Pure Kotlin automated skeleton verification suite
- `scripts/verify_repo.bat` & `scripts/verify_repo.ps1`: Automated test runners

## Test
Executed:
```powershell
powershell -ExecutionPolicy Bypass -File "D:\KUI4\KUI4\scripts\verify_repo.ps1"
```

## Result
PASS (52 checks passed, 0 failed).

## Performance
- Bootstrap test compilation: ~4.1 seconds (cold kotlinc invocation)
- Test execution: ~0.15 seconds

## Problems
None. Local JDK 21 and kotlinc 2.4.20 operate deterministically offline.

## Next
Phase 002 — KUI version (Add version constant and verify `kui --version`).

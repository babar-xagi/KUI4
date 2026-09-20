# Phase 002 — KUI version

## Goal
Add version constant and CLI flag to output version (`kui --version`).

## Learned
- Implemented strongly typed `Version` data class and `KuiVersion` singleton in `platform/kui/cli/KuiVersion.kt`.
- Designed smart incremental compilation in `kui.ps1` and `kui.bat` that caches `kui.jar` and only re-invokes `kotlinc` when source `.kt` files change.
- Cached CLI invocation takes ~500ms on Windows with zero background daemons.

## Files Changed / Added
- `platform/kui/cli/KuiVersion.kt`: Version data model and `0.1.0` constants
- `platform/kui/cli/Main.kt`: Entry point handling `--version`, `-v`, and `version`
- `kui.ps1` & `kui.bat`: Root CLI launcher scripts with smart incremental compilation
- `tests/KuiVersionTest.kt`: Automated verification test suite for version constants, `kui.toml` parity, and CLI outputs
- `scripts/test_version.ps1` & `scripts/test_version.bat`: Automated test runners
- `docs/reports/phase-002-kui-version.md`: Phase completion report

## Test
Executed:
```powershell
powershell -ExecutionPolicy Bypass -File "D:\KUI4\KUI4\scripts\test_version.ps1"
```
And verified direct CLI invocations:
```powershell
.\kui.bat --version
.\kui.bat -v
.\kui.bat version
```

## Result
PASS (7 test assertions passed, 0 failed).
Output matches: `kui version 0.1.0`.

## Performance
- Cold kotlinc build of CLI: ~3.8s
- Cached CLI execution: ~0.5s

## Problems
None. Solved entry-point class selection by explicitly targeting `kui.cli.MainKt` in java execution.

## Next
Phase 003 — CLI entry (Minimal `kui` entry, running without args shows help).

# Phase 003 — CLI entry

## Goal
Provide a minimal `kui` CLI entry point that displays structured help and usage when executed without arguments.

## Learned
- Established centralized CLI help definitions in `platform/kui/cli/Help.kt` aligning with Section 9 of the roadmap.
- Formatted output with clean ASCII characters to ensure universal cross-terminal compatibility across PowerShell, Windows CMD, and POSIX shells.
- Structured CLI command routing in `platform/kui/cli/Main.kt` so running `kui` without arguments immediately surfaces full usage documentation and available commands.

## Files Changed / Added
- `platform/kui/cli/Help.kt`: Centralized structured help text formatter with commands from Section 9
- `platform/kui/cli/Main.kt`: Updated entry point to display help on empty args or help flags
- `tests/CliEntryTest.kt`: Automated verification test suite validating empty-arg help, command presence, flag recognition, and parity across `-h`, `--help`, and `help`
- `scripts/test_cli_entry.ps1` & `scripts/test_cli_entry.bat`: Automated test runners
- `scripts/test_version.ps1`: Upgraded to discover all `platform/kui` Kotlin sources automatically
- `docs/reports/phase-003-cli-entry.md`: Phase completion report

## Test
Executed:
```powershell
powershell -ExecutionPolicy Bypass -File "D:\KUI4\KUI4\scripts\test_cli_entry.ps1"
```
And direct CLI invocation:
```powershell
.\kui.bat
```

## Result
PASS (20 test assertions passed, 0 failed).

## Performance
- Incremental execution: ~0.5s
- Zero daemon overhead

## Problems
Resolved source set discovery across scripts by dynamically discovering all Kotlin source files under `platform/kui/`.

## Next
Phase 004 — Command parser (Parse subcommands; unknown command is friendly).

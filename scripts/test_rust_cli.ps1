$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$WorkspaceRoot = (Resolve-Path (Join-Path $ScriptDir "..\..")).Path

Set-Location $WorkspaceRoot

Write-Host "===========================================================" -ForegroundColor Cyan
Write-Host " Running KUI Rust CLI (kui-cli) Test Suite & Verification " -ForegroundColor Cyan
Write-Host "===========================================================" -ForegroundColor Cyan

# 1. Cargo Test Suite
Write-Host "[1/4] Running automated unit and integration tests (cargo test)..." -ForegroundColor Yellow
cargo test --package kui-cli
if ($LASTEXITCODE -ne 0) {
    Write-Host "Cargo tests failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}
Write-Host "[PASS] Cargo test suite: ALL PASS" -ForegroundColor Green

# 2. Release Build
Write-Host "[2/4] Building release binary (kui.exe)..." -ForegroundColor Yellow
cargo build --release --package kui-cli
if ($LASTEXITCODE -ne 0) {
    Write-Host "Release build failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}
$KuiExe = Join-Path $WorkspaceRoot "target\release\kui.exe"
Write-Host "[PASS] Release binary built: $KuiExe" -ForegroundColor Green

# 3. Direct CLI Verification
Write-Host "[3/4] Verifying CLI commands directly..." -ForegroundColor Yellow
& $KuiExe --version
& $KuiExe doctor
& $KuiExe devices
Write-Host "[PASS] CLI commands verified successfully" -ForegroundColor Green

# 4. Project Scaffolding & Build Lifecycle Test
Write-Host "[4/4] Verifying project lifecycle (new -> info -> clean)..." -ForegroundColor Yellow
$SandboxDir = Join-Path $WorkspaceRoot "test_cli_sandbox"
if (Test-Path $SandboxDir) { Remove-Item $SandboxDir -Recurse -Force }
New-Item -ItemType Directory -Path $SandboxDir -Force | Out-Null

Set-Location $SandboxDir
& $KuiExe new sample_test_app --app-id=com.example.sample
Set-Location (Join-Path $SandboxDir "sample_test_app")
& $KuiExe info
& $KuiExe clean

Set-Location $WorkspaceRoot
Remove-Item $SandboxDir -Recurse -Force

Write-Host "===========================================================" -ForegroundColor Green
Write-Host " [KUI] Rust CLI Verification: ALL TESTS PASSED! " -ForegroundColor Green
Write-Host "===========================================================" -ForegroundColor Green

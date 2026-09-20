$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = (Resolve-Path (Join-Path $ScriptDir "..")).Path

Set-Location $RepoRoot

$BuildDir = Join-Path $RepoRoot ".kui\build"
if (-not (Test-Path $BuildDir)) {
    New-Item -ItemType Directory -Path $BuildDir -Force | Out-Null
}

$JarPath = Join-Path $BuildDir "CliEntryTest.jar"
$KuiSources = Get-ChildItem -Path (Join-Path $RepoRoot "platform\kui") -Recurse -Filter "*.kt" | Select-Object -ExpandProperty FullName
$Sources = $KuiSources + (Join-Path $RepoRoot "tests\CliEntryTest.kt")

Write-Host "[KUI4] Compiling CliEntryTest.kt with kotlinc..." -ForegroundColor Cyan
& kotlinc $Sources -include-runtime -d $JarPath

if ($LASTEXITCODE -ne 0) {
    Write-Host "[KUI4] Compilation failed with code $LASTEXITCODE" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "[KUI4] Executing CLI entry test suite with java..." -ForegroundColor Cyan
& java -cp $JarPath tests.CliEntryTestKt

if ($LASTEXITCODE -ne 0) {
    Write-Host "[KUI4] Test failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "[KUI4] KUI CLI Entry Verification: ALL PASS" -ForegroundColor Green

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = (Resolve-Path (Join-Path $ScriptDir "..")).Path

Set-Location $RepoRoot

$BuildDir = Join-Path $RepoRoot ".kui\build"
if (-not (Test-Path $BuildDir)) {
    New-Item -ItemType Directory -Path $BuildDir -Force | Out-Null
}

$JarPath = Join-Path $BuildDir "CompilerDriverTest.jar"
$KuiSources = @(Get-ChildItem -Path (Join-Path $RepoRoot "platform\kui") -Recurse -Filter "*.kt" | Select-Object -ExpandProperty FullName)
$PlatformUi4Dir = Join-Path $RepoRoot "platform\ui4"
if (Test-Path $PlatformUi4Dir) {
    $KuiSources += @(Get-ChildItem -Path $PlatformUi4Dir -Recurse -Filter "*.kt" | Select-Object -ExpandProperty FullName)
}
$AllSources = $KuiSources + (Join-Path $RepoRoot "tests\CompilerDriverTest.kt")

Write-Host "[KUI4] Compiling CompilerDriverTest.kt with kotlinc..." -ForegroundColor Cyan
& kotlinc $AllSources -include-runtime -d $JarPath

if ($LASTEXITCODE -ne 0) {
    Write-Host "[KUI4] Compilation failed with code $LASTEXITCODE" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "[KUI4] Executing Milestone C Test Suite with java..." -ForegroundColor Cyan
& java -cp $JarPath tests.CompilerDriverTestKt

if ($LASTEXITCODE -ne 0) {
    Write-Host "[KUI4] Test failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "[KUI4] Milestone C Verification: ALL PASS" -ForegroundColor Green

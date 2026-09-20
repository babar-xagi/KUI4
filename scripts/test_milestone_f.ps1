$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = (Resolve-Path (Join-Path $ScriptDir "..")).Path

Set-Location $RepoRoot

$BuildDir = Join-Path $RepoRoot ".kui\build"
if (-not (Test-Path $BuildDir)) {
    New-Item -ItemType Directory -Path $BuildDir -Force | Out-Null
}

$JarPath = Join-Path $BuildDir "UI4MilestoneFTest.jar"
$PlatformKuiDir = Join-Path $RepoRoot "platform\kui"
$PlatformUi4Dir = Join-Path $RepoRoot "platform\ui4"

$AllSources = @(Get-ChildItem -Path $PlatformKuiDir -Recurse -Filter "*.kt" | Select-Object -ExpandProperty FullName)
$AllSources += @(Get-ChildItem -Path $PlatformUi4Dir -Recurse -Filter "*.kt" | Select-Object -ExpandProperty FullName)
$AllSources += (Join-Path $RepoRoot "tests\UI4MilestoneFTest.kt")

Write-Host "[KUI4] Compiling UI4MilestoneFTest.kt with kotlinc..." -ForegroundColor Cyan
& kotlinc $AllSources -include-runtime -d $JarPath

if ($LASTEXITCODE -ne 0) {
    Write-Host "[KUI4] Compilation failed with code $LASTEXITCODE" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "[KUI4] Executing Milestone F Test Battery with java..." -ForegroundColor Cyan
& java -cp $JarPath tests.UI4MilestoneFTestKt

if ($LASTEXITCODE -ne 0) {
    Write-Host "[KUI4] Test failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "[KUI4] Milestone F Verification: ALL PASS" -ForegroundColor Green

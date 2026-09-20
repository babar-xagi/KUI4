$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = (Resolve-Path (Join-Path $ScriptDir "..")).Path

Set-Location $RepoRoot

$BuildDir = Join-Path $RepoRoot ".kui\build"
if (-not (Test-Path $BuildDir)) {
    New-Item -ItemType Directory -Path $BuildDir -Force | Out-Null
}

$JarPath = Join-Path $BuildDir "UI4RenderStateAndInputTest.jar"
$PlatformKuiDir = Join-Path $RepoRoot "platform\kui"
$PlatformUi4Dir = Join-Path $RepoRoot "platform\ui4"

$AllSources = @(Get-ChildItem -Path $PlatformKuiDir -Recurse -Filter "*.kt" | Select-Object -ExpandProperty FullName)
$AllSources += @(Get-ChildItem -Path $PlatformUi4Dir -Recurse -Filter "*.kt" | Select-Object -ExpandProperty FullName)
$AllSources += (Join-Path $RepoRoot "tests\UI4RenderStateAndInputTest.kt")

Write-Host "[KUI4] Compiling UI4RenderStateAndInputTest.kt with kotlinc..." -ForegroundColor Cyan
& kotlinc $AllSources -include-runtime -d $JarPath

if ($LASTEXITCODE -ne 0) {
    Write-Host "[KUI4] Compilation failed with code $LASTEXITCODE" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "[KUI4] Executing Milestone E Test Suite with java..." -ForegroundColor Cyan
& java -cp $JarPath tests.UI4RenderStateAndInputTestKt

if ($LASTEXITCODE -ne 0) {
    Write-Host "[KUI4] Test failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "[KUI4] Milestone E Verification: ALL PASS" -ForegroundColor Green

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = (Resolve-Path (Join-Path $ScriptDir "..")).Path

Set-Location $RepoRoot

$BuildDir = Join-Path $RepoRoot ".kui\build"
if (-not (Test-Path $BuildDir)) {
    New-Item -ItemType Directory -Path $BuildDir -Force | Out-Null
}

$JarPath = Join-Path $BuildDir "KuiVersionTest.jar"
$KuiSources = Get-ChildItem -Path (Join-Path $RepoRoot "platform\kui") -Recurse -Filter "*.kt" | Select-Object -ExpandProperty FullName
$Ui4Sources = if (Test-Path (Join-Path $RepoRoot "platform\ui4")) { Get-ChildItem -Path (Join-Path $RepoRoot "platform\ui4") -Recurse -Filter "*.kt" | Select-Object -ExpandProperty FullName } else { @() }
$Sources = @($KuiSources) + @($Ui4Sources) + (Join-Path $RepoRoot "tests\KuiVersionTest.kt")

Write-Host "[KUI4] Compiling KuiVersionTest.kt with kotlinc..." -ForegroundColor Cyan
& kotlinc $Sources -include-runtime -d $JarPath

if ($LASTEXITCODE -ne 0) {
    Write-Host "[KUI4] Compilation failed with code $LASTEXITCODE" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "[KUI4] Executing version test suite with java..." -ForegroundColor Cyan
& java -cp $JarPath tests.KuiVersionTestKt $RepoRoot

if ($LASTEXITCODE -ne 0) {
    Write-Host "[KUI4] Test failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "[KUI4] KUI Version Verification: ALL PASS" -ForegroundColor Green

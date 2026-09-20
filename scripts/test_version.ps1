$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = (Resolve-Path (Join-Path $ScriptDir "..")).Path

Set-Location $RepoRoot

$BuildDir = Join-Path $RepoRoot ".kui\build"
if (-not (Test-Path $BuildDir)) {
    New-Item -ItemType Directory -Path $BuildDir -Force | Out-Null
}

$JarPath = Join-Path $BuildDir "KuiVersionTest.jar"
$Sources = @(
    (Join-Path $RepoRoot "platform\kui\cli\KuiVersion.kt"),
    (Join-Path $RepoRoot "platform\kui\cli\Main.kt"),
    (Join-Path $RepoRoot "tests\KuiVersionTest.kt")
)

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

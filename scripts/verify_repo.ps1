$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = (Resolve-Path (Join-Path $ScriptDir "..")).Path

Set-Location $RepoRoot

$BuildDir = Join-Path $RepoRoot ".kui\build"
if (-not (Test-Path $BuildDir)) {
    New-Item -ItemType Directory -Path $BuildDir -Force | Out-Null
}

$JarPath = Join-Path $BuildDir "RepositoryBootstrapTest.jar"
$SourcePath = Join-Path $RepoRoot "tests\RepositoryBootstrapTest.kt"

Write-Host "[KUI4] Compiling RepositoryBootstrapTest.kt with kotlinc..." -ForegroundColor Cyan
& kotlinc $SourcePath -include-runtime -d $JarPath

if ($LASTEXITCODE -ne 0) {
    Write-Host "[KUI4] Compilation failed with code $LASTEXITCODE" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "[KUI4] Executing test suite with java..." -ForegroundColor Cyan
& java -jar $JarPath $RepoRoot

if ($LASTEXITCODE -ne 0) {
    Write-Host "[KUI4] Test failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "[KUI4] Repository Bootstrap Verification: ALL PASS" -ForegroundColor Green

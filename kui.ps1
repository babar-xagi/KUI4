$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# 1. Prefer native Rust kui.exe if present
$KuiExe = Join-Path $ScriptDir "kui.exe"
if (-not (Test-Path $KuiExe)) {
    $KuiExe = Join-Path $ScriptDir "bin\kui.exe"
}
if (-not (Test-Path $KuiExe)) {
    $KuiExe = Join-Path $ScriptDir "..\target\release\kui.exe"
}
if (Test-Path $KuiExe) {
    & $KuiExe $args
    exit $LASTEXITCODE
}

# 2. Check for precompiled platform jar
$BuildDir = Join-Path $ScriptDir ".kui\build"
$JarPath = Join-Path $BuildDir "kui.jar"

if (Test-Path $JarPath) {
    & java "-Dkui.home=$ScriptDir" -cp $JarPath kui.cli.MainKt $args
    exit $LASTEXITCODE
}

# 3. Compile platform jar if in source development repository
if (-not (Test-Path $BuildDir)) {
    New-Item -ItemType Directory -Path $BuildDir -Force | Out-Null
}

$PlatformKuiDir = Join-Path $ScriptDir "platform\kui"
$PlatformUi4Dir = Join-Path $ScriptDir "platform\ui4"

$SourceFiles = @(Get-ChildItem -Path $PlatformKuiDir -Recurse -Filter "*.kt" | Select-Object -ExpandProperty FullName)
if (Test-Path $PlatformUi4Dir) {
    $SourceFiles += @(Get-ChildItem -Path $PlatformUi4Dir -Recurse -Filter "*.kt" | Select-Object -ExpandProperty FullName)
}

if ($SourceFiles.Count -eq 0) {
    Write-Error "No Kotlin sources found in $PlatformKuiDir or $PlatformUi4Dir"
    exit 1
}

$SourcesFile = Join-Path $BuildDir "sources_kui.txt"
$SourceFiles | Out-File -Encoding ascii $SourcesFile
& kotlinc "@$SourcesFile" -include-runtime -d $JarPath
if ($LASTEXITCODE -ne 0) {
    Write-Error "KUI compilation failed!"
    exit $LASTEXITCODE
}

# Execute KUI CLI
& java "-Dkui.home=$ScriptDir" -cp $JarPath kui.cli.MainKt $args
exit $LASTEXITCODE

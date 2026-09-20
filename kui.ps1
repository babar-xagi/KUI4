$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

$BuildDir = Join-Path $ScriptDir ".kui\build"
if (-not (Test-Path $BuildDir)) {
    New-Item -ItemType Directory -Path $BuildDir -Force | Out-Null
}

$JarPath = Join-Path $BuildDir "kui.jar"
$PlatformKuiDir = Join-Path $ScriptDir "platform\kui"
$PlatformUi4Dir = Join-Path $ScriptDir "platform\ui4"

# Find all Kotlin source files for KUI toolchain and UI4 runtime
$SourceFiles = @(Get-ChildItem -Path $PlatformKuiDir -Recurse -Filter "*.kt" | Select-Object -ExpandProperty FullName)
if (Test-Path $PlatformUi4Dir) {
    $SourceFiles += @(Get-ChildItem -Path $PlatformUi4Dir -Recurse -Filter "*.kt" | Select-Object -ExpandProperty FullName)
}

if ($SourceFiles.Count -eq 0) {
    Write-Error "No Kotlin sources found in $PlatformKuiDir or $PlatformUi4Dir"
    exit 1
}

# Check if compilation is needed
$NeedCompile = $false
if (-not (Test-Path $JarPath)) {
    $NeedCompile = $true
} else {
    $JarTime = (Get-Item $JarPath).LastWriteTime
    foreach ($src in $SourceFiles) {
        if ((Get-Item $src).LastWriteTime -gt $JarTime) {
            $NeedCompile = $true
            break
        }
    }
}

if ($NeedCompile) {
    & kotlinc $SourceFiles -include-runtime -d $JarPath
    if ($LASTEXITCODE -ne 0) {
        Write-Error "KUI compilation failed!"
        exit $LASTEXITCODE
    }
}

# Execute KUI CLI
& java -cp $JarPath kui.cli.MainKt $args
exit $LASTEXITCODE

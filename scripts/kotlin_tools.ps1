# Shared discovery and compilation. No machine-wide settings are changed.
function Get-KuiFileHash([string]$Path) {
    $Hasher = [Security.Cryptography.SHA256]::Create()
    $Stream = [IO.File]::OpenRead($Path)
    try { return [BitConverter]::ToString($Hasher.ComputeHash($Stream)).Replace('-', '') }
    finally { $Stream.Dispose(); $Hasher.Dispose() }
}

function Initialize-KuiTools {
    $Java = Get-Command java.exe -ErrorAction SilentlyContinue
    if (-not $Java) {
        $Candidates = @()
        if ($env:JAVA_HOME) { $Candidates += Join-Path $env:JAVA_HOME 'bin\java.exe' }
        foreach ($Base in @('C:\Program Files\Eclipse Adoptium', 'C:\Program Files\Java', 'C:\Program Files\Microsoft')) {
            if (Test-Path -LiteralPath $Base) {
                $Candidates += Get-ChildItem -LiteralPath $Base -Directory | Sort-Object LastWriteTime -Descending | ForEach-Object { Join-Path $_.FullName 'bin\java.exe' }
            }
        }
        $JavaPath = $Candidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
        if (-not $JavaPath) { throw 'Java was not found. Install JDK 21 or newer and set JAVA_HOME or PATH.' }
        $env:PATH = "$(Split-Path $JavaPath -Parent);$env:PATH"
    }
    $JavaPath = (Get-Command java.exe -ErrorAction Stop).Source
    $env:JAVA_HOME = Split-Path (Split-Path $JavaPath -Parent) -Parent
    $Kotlinc = Get-Command kotlinc.bat -ErrorAction SilentlyContinue
    if (-not $Kotlinc) {
        $Candidates = @('C:\tools\kotlinc\bin\kotlinc.bat', 'C:\kotlinc\bin\kotlinc.bat')
        foreach ($Variable in @($env:KOTLIN_HOME, $env:KOTLINC_HOME)) {
            if ($Variable) { $Candidates = @(Join-Path $Variable 'bin\kotlinc.bat') + $Candidates }
        }
        $CompilerPath = $Candidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
        if ($CompilerPath) { $env:PATH = "$(Split-Path $CompilerPath -Parent);$env:PATH" }
    }
}

function Write-KotlinSourceList($Sources, [string]$Path) {
    $Lines = @($Sources | ForEach-Object { '"' + $_.FullName.Replace('\', '/') + '"' })
    [System.IO.File]::WriteAllLines($Path, [string[]]$Lines, [System.Text.UTF8Encoding]::new($false))
}

function Get-KuiJar([string]$Root) {
    $DistributedJar = Join-Path $Root 'lib\kui.jar'
    if (Test-Path -LiteralPath $DistributedJar) { return $DistributedJar }
    $Compiler = (Get-Command kotlinc.bat -ErrorAction Stop).Source
    $Sources = @(Get-ChildItem -LiteralPath (Join-Path $Root 'platform') -Recurse -Filter '*.kt' | Sort-Object FullName)
    if ($Sources.Count -eq 0) { throw "No Kotlin platform sources found in $Root" }
    $Build = Join-Path $Root '.kui\build'
    New-Item -ItemType Directory -Path $Build -Force | Out-Null
    $Jar = Join-Path $Build 'kui.jar'
    $Stamp = Join-Path $Build 'kui.sources.sha256'
    $CompilerJar = Join-Path (Split-Path (Split-Path $Compiler -Parent) -Parent) 'lib\kotlin-compiler.jar'
    $Inputs = @($Sources.FullName) + @($Compiler, (Join-Path $Root 'scripts\kotlin_tools.ps1'))
    if (Test-Path -LiteralPath $CompilerJar) { $Inputs += $CompilerJar }
    $Fingerprint = ($Inputs | ForEach-Object { "$($_):$(Get-KuiFileHash $_)" }) -join "`n"
    if (-not (Test-Path -LiteralPath $Jar) -or -not (Test-Path -LiteralPath $Stamp) -or (Get-Content -LiteralPath $Stamp -Raw) -ne $Fingerprint) {
        $ArgFile = Join-Path $Build 'sources_kui.txt'
        Write-KotlinSourceList $Sources $ArgFile
        Write-Host "[KUI] Compiling $($Sources.Count) Kotlin platform sources..."
        & $Compiler "@$ArgFile" -jvm-target 21 -include-runtime -d $Jar
        if ($LASTEXITCODE -ne 0) { throw "Kotlin platform compilation failed (exit $LASTEXITCODE)." }
        [System.IO.File]::WriteAllText($Stamp, $Fingerprint)
    }
    return $Jar
}

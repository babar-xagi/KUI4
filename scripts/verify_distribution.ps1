param([string]$ZipPath = '', [string]$MsiPath = '')
$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path $PSScriptRoot -Parent
. (Join-Path $PSScriptRoot 'kotlin_tools.ps1')
Initialize-KuiTools
$Jar = Get-KuiJar $RepoRoot
$Version = ((& java -cp $Jar kui.cli.MainKt version) -replace '^kui version ', '').Trim()
if ($LASTEXITCODE -ne 0) { throw 'Cannot read platform version.' }
if (-not $ZipPath) { $ZipPath = Join-Path $RepoRoot "dist\kui-$Version-windows.zip" }
$ZipPath = (Resolve-Path -LiteralPath $ZipPath).Path
function Assert-Checksum([string]$Path) {
    $Expected = ([IO.File]::ReadAllText("$Path.sha256").Trim() -split '\s+')[0]
    if ($Expected -ne (Get-KuiFileHash $Path)) { throw "Checksum mismatch: $Path" }
}
Assert-Checksum $ZipPath
Add-Type -AssemblyName System.IO.Compression.FileSystem
$Archive = [IO.Compression.ZipFile]::OpenRead($ZipPath)
try {
    $Names = @($Archive.Entries.FullName)
    foreach ($Required in @('lib/kui.jar', 'kui.bat', 'kui.ps1', 'scripts/kotlin_tools.ps1', 'README.md')) {
        if ($Required -notin $Names) { throw "Missing distribution input: $Required" }
    }
    if ($Names | Where-Object { $_ -match '(^|/)(Cargo\.toml|kui\.exe|\.git|\.kui)(/|$)|\.rs$|\.apk$|(^|/)\.\.(/|$)' }) {
        throw 'Distribution contains legacy code, caches, application outputs, or invalid paths.'
    }
} finally { $Archive.Dispose() }
$ExtractRoot = Join-Path $RepoRoot ('.kui\verification\release package ' + [Guid]::NewGuid().ToString('N'))
[IO.Compression.ZipFile]::ExtractToDirectory($ZipPath, $ExtractRoot)
$Output = @(& (Join-Path $ExtractRoot 'kui.bat') version)
if ($LASTEXITCODE -ne 0 -or ($Output -join "`n").Trim() -ne "kui version $Version") { throw 'Packaged launcher version mismatch.' }
Write-Host "[KUI] ZIP verified: checksum, required files, clean payload, and launcher $Version in a path with spaces."
if ($MsiPath) {
    $MsiPath = (Resolve-Path -LiteralPath $MsiPath).Path
    Assert-Checksum $MsiPath
    $Installer = New-Object -ComObject WindowsInstaller.Installer
    $Database = $null
    $View = $null
    $Record = $null
    try {
        $Database = $Installer.OpenDatabase($MsiPath, 0)
        $View = $Database.OpenView("SELECT ``Value`` FROM ``Property`` WHERE ``Property`` = 'ProductVersion'")
        $View.Execute()
        $Record = $View.Fetch()
        $MsiVersion = $Record.StringData(1)
        if ($MsiVersion -ne $Version) { throw "MSI version $MsiVersion does not match CLI $Version." }
        [void][Runtime.InteropServices.Marshal]::FinalReleaseComObject($Record)
        $Record = $null
        $View.Close()
        [void][Runtime.InteropServices.Marshal]::FinalReleaseComObject($View)
        $View = $Database.OpenView('SELECT `FileName` FROM `File`')
        $View.Execute()
        $FileNames = @()
        while ($null -ne ($Record = $View.Fetch())) {
            $FileNames += ($Record.StringData(1) -split '\|')[-1]
            [void][Runtime.InteropServices.Marshal]::FinalReleaseComObject($Record)
            $Record = $null
        }
        if ('kui.exe' -in $FileNames -or 'Cargo.toml' -in $FileNames -or ($FileNames | Where-Object { $_ -like '*.rs' })) {
            throw 'MSI contains a legacy Rust payload.'
        }
        foreach ($Required in @('kui.jar', 'kui.bat', 'kui.ps1', 'kotlin_tools.ps1')) {
            if ($Required -notin $FileNames) { throw "MSI is missing $Required." }
        }
        Write-Host "[KUI] MSI verified: checksum, ProductVersion $Version, and Kotlin payload."
    } finally {
        foreach ($Object in @($Record, $View, $Database, $Installer)) {
            if ($null -ne $Object) { [void][Runtime.InteropServices.Marshal]::FinalReleaseComObject($Object) }
        }
    }
}

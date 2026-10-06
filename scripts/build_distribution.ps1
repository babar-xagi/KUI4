$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path $PSScriptRoot -Parent
. (Join-Path $PSScriptRoot 'kotlin_tools.ps1')
Initialize-KuiTools
$Jar = Get-KuiJar $RepoRoot
$Dist = Join-Path $RepoRoot 'dist'
$Stage = [IO.Path]::GetFullPath((Join-Path $Dist 'staging'))
if (-not $Stage.StartsWith($RepoRoot + '\', [StringComparison]::OrdinalIgnoreCase)) { throw 'Staging must be inside the repository.' }
if (Test-Path -LiteralPath $Stage) { Remove-Item -LiteralPath $Stage -Recurse -Force }
New-Item -ItemType Directory -Path (Join-Path $Stage 'lib') -Force | Out-Null
Copy-Item -LiteralPath $Jar -Destination (Join-Path $Stage 'lib\kui.jar')
# Explicit input directories also work in source archives without Git.
$Inputs = @()
foreach ($Directory in @('platform', 'docs', 'examples', 'tests', 'benchmarks', 'scripts')) {
    $Inputs += Get-ChildItem -LiteralPath (Join-Path $RepoRoot $Directory) -Recurse -File -Force
}
foreach ($Name in @('kui.bat', 'kui.ps1', 'kui.toml', 'README.md', 'LICENSE', 'UI4_KUI_PURE_KOTLIN_PLATFORM_ROADMAP.md')) {
    $Inputs += Get-Item -LiteralPath (Join-Path $RepoRoot $Name)
}
foreach ($InputFile in $Inputs) {
    $Source = $InputFile.FullName
    $Relative = $Source.Substring($RepoRoot.Length + 1)
    if ($Relative -match '(^|[\\/])(\.kui|build)([\\/]|$)' -and $Relative -like 'examples*') { continue }
    $Target = Join-Path $Stage $Relative
    New-Item -ItemType Directory -Path (Split-Path $Target -Parent) -Force | Out-Null
    Copy-Item -LiteralPath $Source -Destination $Target -Force
}
$Version = ((& java -cp $Jar kui.cli.MainKt version) -replace '^kui version ', '').Trim()
if ($LASTEXITCODE -ne 0) { throw 'Cannot determine KUI version.' }
$Zip = Join-Path $Dist "kui-$Version-windows.zip"
if (Test-Path -LiteralPath $Zip) { Remove-Item -LiteralPath $Zip -Force }
# .NET ZIP includes .gitkeep files and hidden files (Compress-Archive omits them).
Add-Type -AssemblyName System.IO.Compression.FileSystem
[IO.Compression.ZipFile]::CreateFromDirectory($Stage, $Zip)
$Hash = Get-KuiFileHash $Zip
[IO.File]::WriteAllText("$Zip.sha256", "$Hash  $([IO.Path]::GetFileName($Zip))")
Write-Host "[KUI] Kotlin distribution: $Zip"

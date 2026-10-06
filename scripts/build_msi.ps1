param(
    [string]$Version = '',
    [string]$MsiName = ''
)
$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path $PSScriptRoot -Parent
. (Join-Path $PSScriptRoot 'kotlin_tools.ps1')
if (Test-Path -LiteralPath "$env:USERPROFILE\.dotnet\tools\wix.exe") { $env:PATH = "$env:USERPROFILE\.dotnet\tools;$env:PATH" }
$Wix = Get-Command wix -ErrorAction SilentlyContinue
if (-not $Wix) { throw 'WiX is needed only to create an MSI. Use build_distribution.ps1 for a portable Kotlin package.' }
& (Join-Path $PSScriptRoot 'build_distribution.ps1')
$Jar = Get-KuiJar $RepoRoot
$PlatformVersion = ((& java -cp $Jar kui.cli.MainKt version) -replace '^kui version ', '').Trim()
if ($LASTEXITCODE -ne 0) { throw 'Cannot determine KUI version.' }
if (-not $Version) { $Version = $PlatformVersion }
if ($Version -ne $PlatformVersion) { throw "MSI version $Version must match the Kotlin CLI version $PlatformVersion." }
if (-not $MsiName) { $MsiName = "kui-$Version-windows-x64" }
if ($Version -notmatch '^\d+\.\d+\.\d+$' -or $MsiName -notmatch '^[A-Za-z0-9_.-]+$') { throw 'Invalid MSI version or filename.' }
$Dist = Join-Path $RepoRoot 'dist'
$Wxs = Join-Path $Dist 'kui.wxs'
$Definition = @"
<Wix xmlns="http://wixtoolset.org/schemas/v4/wxs">
  <Package Name="KUI Kotlin Platform" Manufacturer="KUI Project" Version="$Version"
           UpgradeCode="D74A1B29-4F58-4C82-962E-73E8A42598D1" Scope="perMachine">
    <MajorUpgrade AllowSameVersionUpgrades="yes" DowngradeErrorMessage="A newer KUI version is already installed." />
    <MediaTemplate EmbedCab="yes" CompressionLevel="high" />
    <StandardDirectory Id="ProgramFiles64Folder">
      <Directory Id="INSTALLFOLDER" Name="KUI" />
    </StandardDirectory>
    <ComponentGroup Id="ProductFiles" Directory="INSTALLFOLDER">
      <Files Include="staging\**" />
      <Component Id="PathEnvironment" Guid="D82E1984-75F2-4EA1-9238-E9E524D1E5A1" KeyPath="yes">
        <Environment Id="KuiPath" Name="PATH" Value="[INSTALLFOLDER]" Part="last" Action="set" System="yes" />
      </Component>
    </ComponentGroup>
    <Feature Id="MainFeature" Title="KUI Kotlin Platform" Level="1">
      <ComponentGroupRef Id="ProductFiles" />
    </Feature>
  </Package>
</Wix>
"@
[IO.File]::WriteAllText($Wxs, $Definition)
$Output = Join-Path $Dist "$MsiName.msi"
Push-Location $Dist
try {
    & $Wix.Source build $Wxs -o $Output -arch x64
    if ($LASTEXITCODE -ne 0) { throw "WiX build failed (exit $LASTEXITCODE)." }
} finally { Pop-Location }
$Hash = Get-KuiFileHash $Output
[IO.File]::WriteAllText("$Output.sha256", "$Hash  $MsiName.msi")
Write-Host "[KUI] Kotlin MSI: $Output"

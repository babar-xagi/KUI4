# ====================================================================
# KUI Platform - MSI Windows Installer Builder (WiX v4/v5)
# Includes Native Rust CLI (kui.exe) + Pure Kotlin Engine (kui.jar)
# ====================================================================

param(
    [string]$Version = "0.2.0",
    [string]$OutputDir = "dist",
    [string]$MsiName = "0.01rs_kui"
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Resolve-Path (Join-Path $ScriptDir "..")
$WorkspaceRoot = Resolve-Path (Join-Path $ScriptDir "..\..")

Write-Host "=== KUI MSI Installer Builder ===" -ForegroundColor Cyan
Write-Host "Project Root:   $ProjectRoot"
Write-Host "Workspace Root: $WorkspaceRoot"
Write-Host "KUI Version:    $Version"
Write-Host "MSI File Name:  $MsiName.msi"

# 1. Verify wix CLI
if (Test-Path "$env:USERPROFILE\.dotnet\tools\wix.exe") {
    $env:PATH = "$env:USERPROFILE\.dotnet\tools;" + $env:PATH
}

$WixCmd = Get-Command wix -ErrorAction SilentlyContinue
if (-not $WixCmd) {
    Write-Error "WiX Toolset CLI ('wix') was not found on PATH. Please install with: dotnet tool install --global wix --version 5.0.2"
    exit 1
}

# 2. Ensure native Rust kui.exe binary exists
Write-Host "[1/5] Verifying native Rust CLI (kui.exe)..." -ForegroundColor Yellow
$KuiExe = Join-Path $ProjectRoot "bin\kui.exe"
if (-not (Test-Path $KuiExe)) {
    $KuiExe = Join-Path $WorkspaceRoot "target\release\kui.exe"
}
if (-not (Test-Path $KuiExe)) {
    Write-Host "Building release kui.exe using cargo..." -ForegroundColor Yellow
    Push-Location $WorkspaceRoot
    cargo build --release --package kui-cli
    Pop-Location
    $KuiExe = Join-Path $WorkspaceRoot "target\release\kui.exe"
}
if (-not (Test-Path $KuiExe)) {
    Write-Error "Failed to locate native kui.exe at $KuiExe"
    exit 1
}
Write-Host "[1/5] Native kui.exe verified: $((Get-Item $KuiExe).Length) bytes" -ForegroundColor Green

# Optional: ensure kui.jar exists (precompiled platform jar)
$KuiJar = Join-Path $ProjectRoot ".kui\build\kui.jar"
if (-not (Test-Path $KuiJar)) {
    Write-Host "Attempting to compile platform kui.jar with kotlinc..." -ForegroundColor Yellow
    $KotlincCmd = $null
    if (Get-Command kotlinc.bat -ErrorAction SilentlyContinue) {
        $KotlincCmd = (Get-Command kotlinc.bat).Source
    } elseif (Get-Command kotlinc -ErrorAction SilentlyContinue) {
        $KotlincCmd = (Get-Command kotlinc).Source
    } elseif (Test-Path "C:\tools\kotlinc\bin\kotlinc.bat") {
        $KotlincCmd = "C:\tools\kotlinc\bin\kotlinc.bat"
    } elseif (Test-Path "$env:LOCALAPPDATA\Programs\IntelliJ IDEA\plugins\Kotlin\kotlinc\bin\kotlinc.bat") {
        $KotlincCmd = "$env:LOCALAPPDATA\Programs\IntelliJ IDEA\plugins\Kotlin\kotlinc\bin\kotlinc.bat"
    }

    if ($KotlincCmd) {
        $PlatformDir = Join-Path $ProjectRoot "platform"
        if (Test-Path $PlatformDir) {
            $Sources = Get-ChildItem -Path $PlatformDir -Recurse -Filter "*.kt" | Select-Object -ExpandProperty FullName
            if ($Sources) {
                $BuildDir = Join-Path $ProjectRoot ".kui\build"
                New-Item -ItemType Directory -Path $BuildDir -Force | Out-Null
                $SourcesFile = Join-Path $env:TEMP "kui_sources_msi_$PID.txt"
                $Sources | Out-File -Encoding ascii $SourcesFile
                Write-Host "Compiling $($Sources.Count) Kotlin sources into kui.jar using $KotlincCmd..." -ForegroundColor Yellow
                & $KotlincCmd "@$SourcesFile" -include-runtime -d $KuiJar
                Remove-Item $SourcesFile -Force -ErrorAction SilentlyContinue
            }
        }
    }
}

if (Test-Path $KuiJar) {
    $KuiJarSize = (Get-Item $KuiJar).Length
    Write-Host "[1/5] Precompiled kui.jar verified: $KuiJarSize bytes" -ForegroundColor Green
} else {
    Write-Host "[1/5] Notice: kui.jar not present. Native Rust packager (kui-packager) operates independently." -ForegroundColor Yellow
}

# 3. Setup Dist and Staging Directory
$DistPath = Join-Path $ProjectRoot $OutputDir
$StagingPath = Join-Path $DistPath "staging"

if (Test-Path $StagingPath) {
    Remove-Item $StagingPath -Recurse -Force
}
New-Item -ItemType Directory -Path $StagingPath -Force | Out-Null

Write-Host "[2/5] Staging distribution payload..." -ForegroundColor Yellow

# Copy root entry files
Copy-Item (Join-Path $ProjectRoot "kui.bat") -Destination $StagingPath -Force
Copy-Item (Join-Path $ProjectRoot "kui.toml") -Destination $StagingPath -Force
Copy-Item (Join-Path $ProjectRoot "LICENSE") -Destination $StagingPath -Force
Copy-Item (Join-Path $ProjectRoot "README.md") -Destination $StagingPath -Force

# Stage native Rust kui.exe at root and bin\
Copy-Item $KuiExe -Destination $StagingPath -Force
$StagingBin = Join-Path $StagingPath "bin"
New-Item -ItemType Directory -Path $StagingBin -Force | Out-Null
Copy-Item $KuiExe -Destination $StagingBin -Force

# Stage a clean, resilient kui.ps1 that directly delegates to kui.exe
$StagingPs1 = @'
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$KuiExe = Join-Path $ScriptDir "kui.exe"
if (-not (Test-Path $KuiExe)) {
    $KuiExe = Join-Path $ScriptDir "bin\kui.exe"
}
if (Test-Path $KuiExe) {
    & $KuiExe $args
    exit $LASTEXITCODE
}
Write-Error "kui.exe not found in $ScriptDir"
exit 1
'@
$StagingPs1 | Out-File -Encoding utf8 (Join-Path $StagingPath "kui.ps1")

# Copy precompiled platform jar into .kui/build (if present)
if (Test-Path $KuiJar) {
    $StagingKuiBuild = Join-Path $StagingPath ".kui\build"
    New-Item -ItemType Directory -Path $StagingKuiBuild -Force | Out-Null
    Copy-Item $KuiJar -Destination $StagingKuiBuild -Force
}

# Copy platform source tree
Copy-Item (Join-Path $ProjectRoot "platform") -Destination $StagingPath -Recurse -Force

# Copy documentation
Copy-Item (Join-Path $ProjectRoot "docs") -Destination $StagingPath -Recurse -Force

# Copy examples (excluding any build artifacts)
$StagingExamples = Join-Path $StagingPath "examples"
New-Item -ItemType Directory -Path $StagingExamples -Force | Out-Null
Copy-Item (Join-Path $ProjectRoot "examples\hello") -Destination $StagingExamples -Recurse -Force

if (Test-Path (Join-Path $ProjectRoot "examples\myaapp")) {
    $StagingMyaapp = Join-Path $StagingExamples "myaapp"
    Copy-Item (Join-Path $ProjectRoot "examples\myaapp") -Destination $StagingExamples -Recurse -Force
    if (Test-Path (Join-Path $StagingMyaapp "build")) {
        Remove-Item (Join-Path $StagingMyaapp "build") -Recurse -Force
    }
    if (Test-Path (Join-Path $StagingMyaapp ".kui\build")) {
        Remove-Item (Join-Path $StagingMyaapp ".kui\build") -Recurse -Force
    }
}

# Copy test verification scripts
Copy-Item (Join-Path $ProjectRoot "scripts") -Destination $StagingPath -Recurse -Force

$StagedFileCount = (Get-ChildItem -Path $StagingPath -Recurse -File).Count
Write-Host "[2/5] Staged $StagedFileCount files into $StagingPath" -ForegroundColor Green

# 4. Generate WiX Definition File (kui.wxs)
Write-Host "[3/5] Generating WiX installer definition..." -ForegroundColor Yellow

$WxsFile = Join-Path $DistPath "kui.wxs"
$WxsContent = @"
<Wix xmlns="http://wixtoolset.org/schemas/v4/wxs" xmlns:ui="http://wixtoolset.org/schemas/v4/wxs/ui">
  <Package Name="KUI Platform ($MsiName)"
           Manufacturer="KUI Project"
           Version="$Version"
           UpgradeCode="D74A1B29-4F58-4C82-962E-73E8A42598D1"
           Scope="perMachine">

    <MajorUpgrade AllowDowngrades="yes" />

    <MediaTemplate EmbedCab="yes" CompressionLevel="high" />

    <!-- Standard Program Files (64-bit) destination -->
    <StandardDirectory Id="ProgramFiles64Folder">
      <Directory Id="INSTALLFOLDER" Name="KUI" />
    </StandardDirectory>

    <!-- Start Menu Shortcuts -->
    <StandardDirectory Id="ProgramMenuFolder">
      <Directory Id="ApplicationProgramsFolder" Name="KUI Platform" />
    </StandardDirectory>

    <!-- Installation Component Groups -->
    <ComponentGroup Id="ProductFilesGroup" Directory="INSTALLFOLDER">
      <!-- Recursively harvests all staged files -->
      <Files Include="staging\**" />

      <!-- Automatic Environment PATH Registration -->
      <Component Id="PathEnvComponent" Guid="D82E1984-75F2-4EA1-9238-E9E524D1E5A1" KeyPath="yes">
        <Environment Id="PATH"
                     Name="PATH"
                     Value="[INSTALLFOLDER]"
                     Permanent="no"
                     Part="last"
                     Action="set"
                     System="yes" />
      </Component>
    </ComponentGroup>

    <!-- Start Menu Shortcut Component -->
    <ComponentGroup Id="ShortcutComponents" Directory="ApplicationProgramsFolder">
      <Component Id="ApplicationShortcut" Guid="8FA72351-38B6-4F8A-98F1-71B0452C9F8A">
        <Shortcut Id="KuiPromptShortcut"
                  Name="KUI Command Prompt"
                  Description="Open a command prompt ready for KUI"
                  Target="[SystemFolder]cmd.exe"
                  Arguments="/k echo Welcome to KUI (Rust + Kotlin) Platform! &amp; kui doctor"
                  WorkingDirectory="PersonalFolder" />
        <RemoveFolder Id="CleanUpShortCut" Directory="ApplicationProgramsFolder" On="uninstall" />
        <RegistryValue Root="HKCU"
                       Key="Software\KUI\Platform"
                       Name="installed"
                       Type="integer"
                       Value="1"
                       KeyPath="yes" />
      </Component>
    </ComponentGroup>

    <!-- Main Feature Hierarchy -->
    <Feature Id="MainFeature" Title="KUI Platform ($MsiName)" Level="1">
      <ComponentGroupRef Id="ProductFilesGroup" />
      <ComponentGroupRef Id="ShortcutComponents" />
    </Feature>

    <!-- Graphical User Interface (InstallDir Wizard) -->
    <ui:WixUI Id="WixUI_InstallDir" InstallDirectory="INSTALLFOLDER" />

  </Package>
</Wix>
"@

$WxsContent | Out-File -Encoding utf8 $WxsFile
Write-Host "[3/5] Written WiX definition to $WxsFile" -ForegroundColor Green

# 5. Build MSI Installer with WiX
Write-Host "[4/5] Compiling MSI installer with WiX..." -ForegroundColor Yellow
$MsiOutput = Join-Path $DistPath "$MsiName.msi"

Push-Location $DistPath
& wix extension add WixToolset.UI.wixext/5.0.2 2>$null | Out-Null

$WixBuildArgs = @(
    "build",
    $WxsFile,
    "-ext", "WixToolset.UI.wixext",
    "-o", $MsiOutput,
    "-arch", "x64"
)

& wix @WixBuildArgs
$WixExit = $LASTEXITCODE
Pop-Location

if ($WixExit -ne 0) {
    Write-Error "WiX build failed with exit code $WixExit"
    exit $WixExit
}

# Also copy to workspace root D:\rust_kot\
$RootCopy = Join-Path $WorkspaceRoot "$MsiName.msi"
Copy-Item $MsiOutput -Destination $RootCopy -Force

# 6. Checksum and Summary
Write-Host "[5/5] Generating cryptographic verification checksum..." -ForegroundColor Yellow
$MsiItem = Get-Item $MsiOutput
$Sha256 = [System.Security.Cryptography.SHA256]::Create()
$Stream = [System.IO.File]::OpenRead($MsiOutput)
$HashBytes = $Sha256.ComputeHash($Stream)
$Stream.Close()
$Hash = [System.BitConverter]::ToString($HashBytes).Replace("-", "").ToUpper()
$HashFile = "$MsiOutput.sha256"
"$Hash  $($MsiItem.Name)" | Out-File -Encoding ascii $HashFile

$RootHashFile = "$RootCopy.sha256"
"$Hash  $($MsiItem.Name)" | Out-File -Encoding ascii $RootHashFile

Write-Host "`n========================================================" -ForegroundColor Green
Write-Host " KUI MSI Installer Built Successfully! 🎉" -ForegroundColor Green
Write-Host "========================================================" -ForegroundColor Green
Write-Host " Installer File: $($MsiItem.FullName)"
Write-Host " Root Copy:     $RootCopy"
Write-Host " Size:           $([math]::Round($MsiItem.Length / 1MB, 2)) MB ($($MsiItem.Length) bytes)"
Write-Host " Architecture:   x64"
Write-Host " Target OS:      Windows 10 / Windows 11 (64-bit)"
Write-Host " SHA-256 Hash:   $Hash"
Write-Host "========================================================`n"

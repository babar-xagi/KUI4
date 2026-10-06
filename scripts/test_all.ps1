param([string[]]$Suite = @())
$Suite = @($Suite | ForEach-Object { $_ -split ',' } | Where-Object { $_ })
$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path $PSScriptRoot -Parent
. (Join-Path $PSScriptRoot 'kotlin_tools.ps1')
try {
    Initialize-KuiTools
    $PlatformJar = Get-KuiJar $RepoRoot
    $TestSources = @(Get-ChildItem -LiteralPath (Join-Path $RepoRoot 'tests') -Filter '*.kt' | Sort-Object Name)
    $TestDir = Join-Path $RepoRoot '.kui\verification'
    New-Item -ItemType Directory -Path $TestDir -Force | Out-Null
    $ArgFile = Join-Path $TestDir 'test-sources.txt'
    $TestJar = Join-Path $TestDir 'tests.jar'
    Write-KotlinSourceList $TestSources $ArgFile
    [IO.File]::AppendAllText($ArgFile, '"-Xfriend-paths=' + $PlatformJar.Replace('\', '/') + '"' + "`n")
    & kotlinc.bat "@$ArgFile" -cp $PlatformJar -jvm-target 21 -d $TestJar
    if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed.' }
    $Failed = @()
    $Selected = @($TestSources | Where-Object { $Suite.Count -eq 0 -or $_.BaseName -in $Suite })
    if ($Selected.Count -eq 0 -or ($Suite.Count -gt 0 -and $Selected.Count -ne $Suite.Count)) { throw 'Unknown or duplicate test suite name.' }
    Push-Location $RepoRoot
    try {
        foreach ($Test in $Selected) {
            $ClassName = "tests.$($Test.BaseName)Kt"
            $Log = Join-Path $TestDir "$($Test.BaseName).log"
            Write-Host "[KUI] Running $($Test.BaseName)..."
            $ErrorActionPreference = 'Continue'
            & java -ea "-Dkui.home=$RepoRoot" -cp "$TestJar;$PlatformJar" $ClassName $RepoRoot 2>&1 | ForEach-Object { $_.ToString() } | Tee-Object -FilePath $Log
            $ErrorActionPreference = 'Stop'
            if ($LASTEXITCODE -ne 0) { $Failed += $Test.BaseName }
        }
    } finally { Pop-Location }
    Write-Host "[KUI] $($Selected.Count - $Failed.Count)/$($Selected.Count) suites passed. Logs: $TestDir"
    if ($Failed.Count -gt 0) { throw "Failed suites: $($Failed -join ', ')" }
    exit 0
} catch {
    Write-Error $_ -ErrorAction Continue
    exit 1
}

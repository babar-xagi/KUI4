$ErrorActionPreference = 'Stop'
$RepoRoot = $PSScriptRoot
. (Join-Path $RepoRoot 'scripts\kotlin_tools.ps1')
try {
    Initialize-KuiTools
    $JarPath = Get-KuiJar $RepoRoot
    & java "-Dkui.home=$RepoRoot" -cp $JarPath kui.cli.MainKt @args
    exit $LASTEXITCODE
} catch {
    Write-Error $_ -ErrorAction Continue
    exit 1
}

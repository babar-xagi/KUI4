$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = (Resolve-Path (Join-Path $ScriptDir "..")).Path

Set-Location $RepoRoot

$BuildDir = Join-Path $RepoRoot ".kui\build"
if (-not (Test-Path $BuildDir)) {
    New-Item -ItemType Directory -Path $BuildDir -Force | Out-Null
}

$KuiSources = Get-ChildItem -Path (Join-Path $RepoRoot "platform\kui") -Recurse -Filter "*.kt" | Select-Object -ExpandProperty FullName

$TestSuites = @(
    @{ Name = "Phase 001: RepositoryBootstrapTest"; Source = "tests\RepositoryBootstrapTest.kt"; Class = "tests.RepositoryBootstrapTestKt"; Args = @($RepoRoot) },
    @{ Name = "Phase 002: KuiVersionTest";           Source = "tests\KuiVersionTest.kt";           Class = "tests.KuiVersionTestKt";           Args = @($RepoRoot) },
    @{ Name = "Phase 003: CliEntryTest";             Source = "tests\CliEntryTest.kt";             Class = "tests.CliEntryTestKt";             Args = @() },
    @{ Name = "Phase 004-005: CommandParserTest";    Source = "tests\CommandParserTest.kt";        Class = "tests.CommandParserTestKt";        Args = @() },
    @{ Name = "Phase 006-009: ProjectConfigTest";    Source = "tests\ProjectConfigTest.kt";        Class = "tests.ProjectConfigTestKt";        Args = @($RepoRoot) },
    @{ Name = "Phase 010-011: LoggingAndTimingTest"; Source = "tests\LoggingAndTimingTest.kt";     Class = "tests.LoggingAndTimingTestKt";     Args = @() },
    @{ Name = "Phase 012-013: HashingTest";          Source = "tests\HashingTest.kt";              Class = "tests.HashingTestKt";              Args = @() },
    @{ Name = "Phase 014-015: BuildAndCacheDirsTest";Source = "tests\BuildAndCacheDirectoriesTest.kt"; Class = "tests.BuildAndCacheDirectoriesTestKt"; Args = @() },
    @{ Name = "Phase 016-019: TaskGraphAndCacheTest";Source = "tests\TaskGraphAndCacheTest.kt";    Class = "tests.TaskGraphAndCacheTestKt";    Args = @() },
    @{ Name = "Phase 020: FoundationBenchmarkTest";  Source = "tests\FoundationBenchmarkTest.kt";  Class = "tests.FoundationBenchmarkTestKt";  Args = @($RepoRoot) }
)

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host " KUI4 Milestone A - Complete Test Battery (001-020)" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

$PassedCount = 0
$FailedCount = 0

foreach ($suite in $TestSuites) {
    Write-Host "`n>>> Running $($suite.Name)..." -ForegroundColor Yellow
    $JarName = (Split-Path -Leaf $suite.Source).Replace(".kt", ".jar")
    $JarPath = Join-Path $BuildDir $JarName
    $AllSources = $KuiSources + (Join-Path $RepoRoot $suite.Source)

    & kotlinc $AllSources -include-runtime -d $JarPath
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  [FAIL] Compilation error in $($suite.Name)" -ForegroundColor Red
        $FailedCount++
        continue
    }

    & java -cp $JarPath $suite.Class @($suite.Args)
    if ($LASTEXITCODE -eq 0) {
        $PassedCount++
    } else {
        $FailedCount++
        Write-Host "  [FAIL] $($suite.Name) exited with code $LASTEXITCODE" -ForegroundColor Red
    }
}

$resultColor = if ($FailedCount -eq 0) { "Green" } else { "Red" }
Write-Host "`n==================================================" -ForegroundColor Cyan
Write-Host " Milestone A Results: $PassedCount Suites PASSED, $FailedCount FAILED" -ForegroundColor $resultColor
Write-Host "==================================================" -ForegroundColor Cyan

if ($FailedCount -gt 0) {
    exit 1
}

package tests

import kui.build.BuildDirectories
import kui.build.CleanCommand
import kui.build.KuiLogger
import kui.build.TaskExecutionContext
import kui.build.TaskStatus
import kui.cache.CacheDirectories
import kui.compiler.ClasspathModel
import kui.compiler.CompilerBenchmark
import kui.compiler.CompilerCacheKey
import kui.compiler.CompilerDiscovery
import kui.compiler.CompileTask
import kui.compiler.DiagnosticParser
import kui.compiler.DiagnosticSeverity
import kui.compiler.KotlinCompiler
import kui.compiler.KotlinProcessRunner
import kui.compiler.SourceDiscovery
import kui.compiler.SourceHasher
import kui.compiler.UI4Bootstrap
import kui.doctor.DoctorCommand
import java.io.File

/**
 * Complete Verification Test Suite for Milestone C: Kotlin Compiler Driver 🧠 (Phases 036–055).
 */
fun main() {
    println("==================================================")
    println(" Milestone C: Kotlin Compiler Driver Tests (036-055)")
    println("==================================================")

    var passed = 0
    var failed = 0

    fun check(name: String, condition: Boolean, details: String = "") {
        if (condition) {
            println("  [PASS] $name")
            passed++
        } else {
            println("  [FAIL] $name: $details")
            failed++
        }
    }

    val tempRoot = File(System.getProperty("java.io.tmpdir"), "kui_milestone_c_test_" + System.currentTimeMillis())
    tempRoot.mkdirs()

    try {
        // -------------------------------------------------------------
        // 1. Phase 036 & Phase 037: Find Kotlin Compiler & Read Version
        // -------------------------------------------------------------
        val kotlinc = CompilerDiscovery.findKotlinc()
        check("Discover kotlinc compiler", kotlinc.isValid && kotlinc.path != "NOT_FOUND", "Path: ${kotlinc.path}")
        check("Kotlinc version is >= 2.0.0", kotlinc.version.startsWith("2."), "Version: ${kotlinc.version}")

        // -------------------------------------------------------------
        // 2. Phase 038: JDK Check & Doctor Command
        // -------------------------------------------------------------
        val javaInfo = CompilerDiscovery.findJava()
        check("Discover Java Runtime (JDK >= 17)", javaInfo.isValid, "Java: ${javaInfo.version}")
        val envReport = CompilerDiscovery.checkEnvironment()
        check("Environment check isReady is true", envReport.isReady, "Errors: ${envReport.errors}")
        val doctorExit = DoctorCommand.execute()
        check("DoctorCommand executes with exit code 0", doctorExit == 0)

        // -------------------------------------------------------------
        // 3. Phase 039: Process Wrapper
        // -------------------------------------------------------------
        val procResult = KotlinProcessRunner.run(listOf(javaInfo.path, "-version"))
        check("ProcessRunner captures exitCode 0", procResult.isSuccess)
        check("ProcessRunner measures durationMs > 0", procResult.durationMs >= 0)
        check("ProcessRunner captures output", procResult.stdout.isNotEmpty() || procResult.stderr.isNotEmpty())

        // -------------------------------------------------------------
        // 4. Phase 040: Compile One File
        // -------------------------------------------------------------
        val singleSrcDir = File(tempRoot, "single/src").apply { mkdirs() }
        val singleOutDir = File(tempRoot, "single/out").apply { mkdirs() }
        val singleFile = File(singleSrcDir, "Simple.kt").apply {
            writeText("package test\nclass Simple { val value = 42 }\n")
        }

        val singleResult = KotlinCompiler.compile(
            sources = listOf(singleFile),
            outputDir = singleOutDir,
            kotlincPath = kotlinc.path
        )
        check("Single file compilation succeeds", singleResult.isSuccess, singleResult.rawOutput)
        val classFile = File(singleOutDir, "test/Simple.class")
        check("Generated .class file exists", classFile.exists() && classFile.length() > 0)

        // -------------------------------------------------------------
        // 5. Phase 041 & Phase 052: Diagnostics & Failure UX
        // -------------------------------------------------------------
        val sampleKotlincOutput = """
            D:\project\src\main.kt:14:5: error: unresolved reference 'foo'
            D:\project\src\main.kt:20:9: warning: variable 'x' is never used
            e: D:\project\src\other.kt: (3, 7): type mismatch
        """.trimIndent()

        val parsedDiagnostics = DiagnosticParser.parse(sampleKotlincOutput)
        check("DiagnosticParser parsed 3 diagnostics", parsedDiagnostics.size == 3)
        check("First diagnostic is ERROR on line 14 col 5",
            parsedDiagnostics[0].severity == DiagnosticSeverity.ERROR &&
            parsedDiagnostics[0].line == 14 && parsedDiagnostics[0].column == 5
        )
        check("Second diagnostic is WARNING", parsedDiagnostics[1].severity == DiagnosticSeverity.WARNING)
        check("Third diagnostic is ERROR from prefixed format",
            parsedDiagnostics[2].severity == DiagnosticSeverity.ERROR &&
            parsedDiagnostics[2].line == 3 && parsedDiagnostics[2].column == 7
        )

        val concise = DiagnosticParser.formatConcise(parsedDiagnostics, File("D:/project"))
        check("Concise failure UX removes project root prefix",
            concise.contains("src/main.kt:14:5: error:") && !concise.contains("D:/project"),
            "Concise output:\n$concise"
        )

        // -------------------------------------------------------------
        // 6. Phase 042 & Phase 043: Source Discovery & Multiple Files
        // -------------------------------------------------------------
        val multiSrcDir = File(tempRoot, "multi/src").apply { mkdirs() }
        val multiOutDir = File(tempRoot, "multi/out").apply { mkdirs() }
        val subDir = File(multiSrcDir, "utils").apply { mkdirs() }

        val fileB = File(subDir, "Helper.kt").apply {
            writeText("package multi.utils\nfun getGreeting(): String = \"Hello Cross-File\"\n")
        }
        val fileA = File(multiSrcDir, "App.kt").apply {
            writeText("package multi\nimport multi.utils.getGreeting\nfun main() { println(getGreeting()) }\n")
        }

        val discovered = SourceDiscovery.findSources(multiSrcDir)
        check("SourceDiscovery finds 2 files", discovered.size == 2)
        check("SourceDiscovery sorted deterministically",
            discovered[0].name == "App.kt" && discovered[1].name == "Helper.kt"
        )

        val multiResult = KotlinCompiler.compile(
            sources = discovered,
            outputDir = multiOutDir,
            kotlincPath = kotlinc.path
        )
        check("Cross-file multiple sources compilation succeeds", multiResult.isSuccess, multiResult.rawOutput)
        check("Output AppKt.class exists", File(multiOutDir, "multi/AppKt.class").exists())
        check("Output HelperKt.class exists", File(multiOutDir, "multi/utils/HelperKt.class").exists())

        // -------------------------------------------------------------
        // 7. Phase 044: Classpath Model
        // -------------------------------------------------------------
        val dummyJar = File(tempRoot, "dummy.jar").apply { writeText("dummy-jar-content") }
        val cpModel = ClasspathModel()
        cpModel.add(dummyJar, "Dummy Dependency")
        check("ClasspathModel has 1 entry", cpModel.getEntries().size == 1)
        check("Classpath string contains dummy jar", cpModel.asClasspathString().contains("dummy.jar"))
        val cpFingerprint = cpModel.computeFingerprint()
        check("Classpath fingerprint is 64-char SHA-256", cpFingerprint.length == 64)

        // -------------------------------------------------------------
        // 8. Phase 045: UI4 API Bootstrap
        // -------------------------------------------------------------
        val ui4BuildDir = File(tempRoot, "ui4_build").apply { mkdirs() }
        val ui4Jar = UI4Bootstrap.ensureApiJar(ui4BuildDir, kotlinc.path)
        check("UI4Bootstrap created ui4-api.jar", ui4Jar.exists() && ui4Jar.length() > 0)

        // Fast cache check: calling again returns immediately without recompiling
        val cachedUi4Jar = UI4Bootstrap.ensureApiJar(ui4BuildDir, kotlinc.path)
        check("UI4Bootstrap reuses cached jar", cachedUi4Jar.canonicalPath == ui4Jar.canonicalPath)

        // Verify user code referencing UI4 compiles against this jar
        val ui4AppDir = File(tempRoot, "ui4_app/src").apply { mkdirs() }
        val ui4OutDir = File(tempRoot, "ui4_app/out").apply { mkdirs() }
        val ui4Main = File(ui4AppDir, "Main.kt").apply {
            writeText("""
                import ui4.*
                fun main() = app {
                    screen {
                        center {
                            text("Test UI4 Bootstrap")
                        }
                    }
                }
            """.trimIndent())
        }

        val ui4Classpath = ClasspathModel().add(ui4Jar)
        val ui4CompileResult = KotlinCompiler.compile(
            sources = listOf(ui4Main),
            outputDir = ui4OutDir,
            classpath = ui4Classpath,
            kotlincPath = kotlinc.path
        )
        check("App importing UI4 compiles cleanly against ui4-api.jar",
            ui4CompileResult.isSuccess, ui4CompileResult.rawOutput
        )

        // -------------------------------------------------------------
        // 9. Phase 046 & Phase 047: Source Hasher & Compiler Cache Key
        // -------------------------------------------------------------
        val hash1 = SourceHasher.hashSources(discovered, multiSrcDir)
        val hash2 = SourceHasher.hashSources(discovered, multiSrcDir)
        check("SourceHasher produces identical hash for unchanged files", hash1 == hash2)
        check("SourceHasher output is 64-char SHA-256", hash1.length == 64)

        val key1 = CompilerCacheKey.compute(
            sources = discovered,
            sourceBaseDir = multiSrcDir,
            classpath = cpModel,
            compilerVersion = kotlinc.version
        )
        val key2 = CompilerCacheKey.compute(
            sources = discovered,
            sourceBaseDir = multiSrcDir,
            classpath = cpModel,
            compilerVersion = kotlinc.version
        )
        check("CompilerCacheKey is deterministic", key1.key == key2.key)

        val modifiedKey = CompilerCacheKey.compute(
            sources = discovered,
            sourceBaseDir = multiSrcDir,
            classpath = cpModel,
            compilerVersion = "2.5.0-fake"
        )
        check("CompilerCacheKey changes when compiler version changes", key1.key != modifiedKey.key)

        // -------------------------------------------------------------
        // 10. Phase 048 & Phase 051: CompileTask & Timing
        // -------------------------------------------------------------
        val taskProjectRoot = File(tempRoot, "task_proj").apply { mkdirs() }
        val taskSrcDir = File(taskProjectRoot, "src").apply { mkdirs() }
        File(taskSrcDir, "TaskApp.kt").writeText("fun runTask() = println(\"task\")")

        val taskBuildDirs = BuildDirectories(taskProjectRoot).apply { ensureCreated() }
        val compileTask = CompileTask(
            sources = listOf(File(taskSrcDir, "TaskApp.kt")),
            classpath = ClasspathModel(),
            kotlincPath = kotlinc.path
        )

        val taskExecContext = TaskExecutionContext(
            projectRoot = taskProjectRoot,
            buildDirs = taskBuildDirs,
            logger = KuiLogger(level = kui.build.LogLevel.INFO)
        )

        val taskResult = compileTask.execute(taskExecContext)
        check("CompileTask status is SUCCESS", taskResult.status == TaskStatus.SUCCESS)
        check("CompileTask records durationMs", taskResult.durationMs >= 0)
        check("CompileTask produced output class", File(taskBuildDirs.classesDir, "TaskAppKt.class").exists())

        // -------------------------------------------------------------
        // 11. Phase 050: Clean Command
        // -------------------------------------------------------------
        val cleanSuccess = taskBuildDirs.clean()
        check("BuildDirectories.clean removes build artifacts", cleanSuccess && !taskBuildDirs.classesDir.exists())

        // -------------------------------------------------------------
        // 12. Phase 053: Verbose Mode
        // -------------------------------------------------------------
        val verboseResult = KotlinCompiler.compile(
            sources = listOf(singleFile),
            outputDir = singleOutDir,
            kotlincPath = kotlinc.path,
            verbose = true
        )
        check("Verbose compilation includes -verbose flag in command line",
            verboseResult.commandLine.contains("-verbose")
        )

        // -------------------------------------------------------------
        // 13. Phase 054: Integration Fixture (examples/hello)
        // -------------------------------------------------------------
        val helloRoot = File("examples/hello").canonicalFile
        check("examples/hello project exists", helloRoot.exists() && File(helloRoot, "kui.toml").exists())
        val helloBuildResult = kui.build.BuildCommand.execute(projectRootOverride = helloRoot)
        check("examples/hello builds successfully (exit 0)", helloBuildResult == 0)
        val helloClasses = File(helloRoot, ".kui/build/classes/MainKt.class")
        check("examples/hello builds and produces MainKt.class", helloClasses.exists())

        // -------------------------------------------------------------
        // 14. Phase 055: Compiler Baseline Benchmark
        // -------------------------------------------------------------
        val benchStats = CompilerBenchmark.run(helloRoot, warmIterations = 2)
        check("CompilerBenchmark coldCompileMs > 0", benchStats.coldCompileMs > 0)
        check("CompilerBenchmark warmCompileMs > 0", benchStats.warmCompileMs > 0)
        check("CompilerBenchmark iterations count matches", benchStats.iterations == 2)
        val benchReportFile = File(helloRoot, ".kui/build/reports/benchmark-compiler.json")
        check("CompilerBenchmark report JSON created", benchReportFile.exists() && benchReportFile.length() > 0)

    } finally {
        tempRoot.deleteRecursively()
    }

    println("==================================================")
    println(" Milestone C Results: $passed Passed, $failed Failed")
    println("==================================================")

    if (failed > 0) {
        System.exit(1)
    }
}

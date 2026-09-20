package tests

import kui.build.Stopwatch
import kui.config.ConfigDiagnostics
import kui.config.ConfigValidationResult
import kui.config.TomlReader
import kui.project.NewProjectOptions
import kui.project.ProjectGenerator
import java.io.File

/**
 * Complete Verification Test Suite for Milestone B: Project Generator 📦 (Phases 021-035).
 */
fun main() {
    println("==================================================")
    println(" Milestone B: Project Generator Tests (021-035)")
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

    val tempTestRoot = File(System.getProperty("java.io.tmpdir"), "kui_milestone_b_test_" + System.currentTimeMillis())
    tempTestRoot.mkdirs()

    try {
        // 1. Phase 021: Project Name Validation
        check("Rejects empty project name", ProjectGenerator.validateProjectName("") != null)
        check("Rejects name with spaces", ProjectGenerator.validateProjectName("my app") != null)
        check("Rejects name with special chars", ProjectGenerator.validateProjectName("app@123") != null)
        check("Rejects name starting with digit", ProjectGenerator.validateProjectName("123app") != null)
        check("Accepts valid alphanumeric name", ProjectGenerator.validateProjectName("my-cool_app") == null)

        // 2. Phase 028 & Phase 029: Application ID Inference and Custom Override
        val defaultOptions = NewProjectOptions("my-awesome-app", File(tempTestRoot, "my-awesome-app"))
        check("Default App ID infer rule 'com.example.my_awesome_app'",
            defaultOptions.resolveApplicationId() == "com.example.my_awesome_app"
        )

        val customOptions = NewProjectOptions(
            "custom-app",
            File(tempTestRoot, "custom-app"),
            applicationId = "org.kui4.custom"
        )
        check("Custom App ID retained", customOptions.resolveApplicationId() == "org.kui4.custom")

        // 3. Phase 022-032: Full Standard Project Generation
        val stdTarget = File(tempTestRoot, "standard-demo")
        val stdOptions = NewProjectOptions(
            name = "standard-demo",
            targetDir = stdTarget,
            version = "0.2.0",
            versionCode = 2,
            minSdk = 26,
            targetSdk = 36
        )

        val genResult = ProjectGenerator.generate(stdOptions)
        check("Generation returns success", genResult.success)
        check("Phase 022: Root folder created", stdTarget.isDirectory)

        // Phase 023: Generate and parse kui.toml
        val kuiToml = File(stdTarget, "kui.toml")
        check("Phase 023: kui.toml exists", kuiToml.isFile)
        val doc = TomlReader.parse(kuiToml)
        check("kui.toml name matches", doc.getString("project", "name") == "standard-demo")
        check("Phase 030: Version matches '0.2.0'", doc.getString("project", "version") == "0.2.0")
        check("Phase 030: Version code matches 2", doc.getInt("project", "version_code") == 2)
        check("Phase 031: Min SDK matches 26", doc.getInt("android", "min_sdk") == 26)
        check("Phase 031: Target SDK matches 36", doc.getInt("android", "target_sdk") == 36)
        check("Phase 032: Generator version recorded", doc.getString("project", "generator_version") == "0.1.0")

        // Phase 024: Generate main.kt
        val mainKt = File(stdTarget, "src/main.kt")
        check("Phase 024: src/main.kt exists", mainKt.isFile)
        check("src/main.kt contains UI4 DSL", mainKt.readText().contains("import ui4.*") && mainKt.readText().contains("screen {"))

        // Phase 025: Assets folders
        check("Phase 025: assets/images exists", File(stdTarget, "assets/images").isDirectory)
        check("Phase 025: assets/fonts exists", File(stdTarget, "assets/fonts").isDirectory)

        // Phase 026: Tests folder
        val testKt = File(stdTarget, "tests/AppTest.kt")
        check("Phase 026: tests/AppTest.kt exists", testKt.isFile)
        check("tests/AppTest.kt has valid content", testKt.readText().contains("fun main()"))

        // Phase 027 & 035: README with clear structure
        val readme = File(stdTarget, "README.md")
        check("Phase 027: README.md exists", readme.isFile)
        check("Phase 035: README explains project layout", readme.readText().contains("kui.toml") && readme.readText().contains("kui run"))

        // Collision check
        val collisionResult = ProjectGenerator.generate(stdOptions)
        check("Collision detection rejects overwriting existing project", !collisionResult.success)

        // 4. Phase 033: Minimal Template Generation (--minimal)
        val minimalTarget = File(tempTestRoot, "minimal-demo")
        val minOptions = NewProjectOptions(
            name = "minimal-demo",
            targetDir = minimalTarget,
            minimal = true
        )
        val minResult = ProjectGenerator.generate(minOptions)
        check("Minimal project generation succeeds", minResult.success)
        check("Minimal has kui.toml", File(minimalTarget, "kui.toml").isFile)
        check("Minimal has src/main.kt", File(minimalTarget, "src/main.kt").isFile)
        check("Minimal omits assets/ folder", !File(minimalTarget, "assets").exists())
        check("Minimal omits tests/ folder", !File(minimalTarget, "tests").exists())

        // 5. Phase 034: Generator Integration & Diagnostics Validation
        when (val v1 = ConfigDiagnostics.validateAndLoad(stdTarget)) {
            is ConfigValidationResult.Success -> check("Standard generated project passes ConfigDiagnostics", true)
            is ConfigValidationResult.Failure -> check("Standard generated project passes ConfigDiagnostics", false, v1.formatErrorMessage())
        }
        when (val v2 = ConfigDiagnostics.validateAndLoad(minimalTarget)) {
            is ConfigValidationResult.Success -> check("Minimal generated project passes ConfigDiagnostics", true)
            is ConfigValidationResult.Failure -> check("Minimal generated project passes ConfigDiagnostics", false, v2.formatErrorMessage())
        }

        // 6. Phase 035: Project Generation Speed Benchmark
        val benchmarkTargetBase = File(tempTestRoot, "benchmarks").apply { mkdirs() }
        val iterations = 20
        var totalMs = 0L

        for (i in 1..iterations) {
            val dir = File(benchmarkTargetBase, "bench-$i")
            val opts = NewProjectOptions("bench-$i", dir)
            val (_, ms) = Stopwatch.measure { ProjectGenerator.generate(opts) }
            totalMs += ms
        }

        val avgMs = totalMs.toDouble() / iterations
        println("  Average project generation time across $iterations runs: ${"%.2f".format(avgMs)} ms")
        check("Project generation is fast (avg < 50ms)", avgMs < 50.0, "Average took ${avgMs}ms")

    } finally {
        tempTestRoot.deleteRecursively()
    }

    println("--------------------------------------------------")
    println("Summary: $passed PASSED, $failed FAILED")
    println("==================================================")

    if (failed > 0) System.exit(1) else println("RESULT: PASS")
}

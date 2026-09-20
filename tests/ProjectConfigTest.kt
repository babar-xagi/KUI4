package tests

import kui.config.ConfigDiagnostics
import kui.config.ConfigValidationResult
import kui.config.TomlReader
import kui.project.InfoCommand
import kui.project.ProjectFinder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

/**
 * Verification test for Phases 006, 007, 008, 009:
 * Project Path Detection, TOML Reader, Config Diagnostics, and Info Command.
 */
fun main(args: Array<String>) {
    println("==================================================")
    println(" Phases 006-009: Project & Config Verification")
    println("==================================================")

    val root = if (args.isNotEmpty()) File(args[0]) else File(".").canonicalFile
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

    // 1. Phase 006: Project Root Detection
    val foundFromRoot = ProjectFinder.findProjectRoot(root)
    check("Finds project root from repo root", foundFromRoot?.canonicalPath == root.canonicalPath)

    val helloDir = File(root, "examples/hello")
    val foundFromHello = ProjectFinder.findProjectRoot(helloDir)
    check("Finds project root from subdirectory", foundFromHello?.canonicalPath == helloDir.canonicalPath)

    val tempNonProjectDir = File(root, ".kui")
    val foundFromTemp = ProjectFinder.findProjectRoot(tempNonProjectDir)
    check("Traverses parent directories to locate kui.toml", foundFromTemp?.canonicalPath == root.canonicalPath)

    // 2. Phase 007: TOML Reader v1
    val sampleToml = """
        # Sample configuration fixture
        [project]
        name = "test-app"
        version = "1.2.3"
        application_id = "org.kui4.testapp"

        [android]
        min_sdk = 26
        target_sdk = 36

        [ui]
        theme = "dark"
    """.trimIndent()

    val doc = TomlReader.parse(sampleToml)
    check("TOML string parsed correctly", doc.getString("project", "name") == "test-app")
    check("TOML version parsed correctly", doc.getString("project", "version") == "1.2.3")
    check("TOML integer parsed correctly", doc.getInt("android", "min_sdk") == 26)
    check("TOML section parsed correctly", doc.getString("ui", "theme") == "dark")

    // 3. Phase 008: Config Diagnostics
    // Valid case:
    when (val valid = ConfigDiagnostics.validateAndLoad(helloDir)) {
        is ConfigValidationResult.Success -> {
            check("Hello project config valid", valid.config.project.name == "hello")
            check("Hello project targetSdk is 36", valid.config.android.targetSdk == 36)
        }
        is ConfigValidationResult.Failure -> {
            check("Hello project config valid", false, valid.formatErrorMessage())
        }
    }

    // Invalid config case (bad SDK & missing version)
    val invalidDir = File(root, ".kui/build/temp_test_invalid_project")
    invalidDir.mkdirs()
    try {
        File(invalidDir, "kui.toml").writeText("""
            [project]
            name = "invalid app!"

            [android]
            min_sdk = 40
            target_sdk = 25
        """.trimIndent())

        when (val invalid = ConfigDiagnostics.validateAndLoad(invalidDir)) {
            is ConfigValidationResult.Failure -> {
                check("Detects invalid project name format", invalid.issues.any { it.field == "project.name" })
                check("Detects missing required version", invalid.issues.any { it.field == "project.version" })
                check("Detects target_sdk smaller than min_sdk", invalid.issues.any { it.field == "android.target_sdk" })
            }
            is ConfigValidationResult.Success -> {
                check("Fails on invalid config", false, "Expected failure but passed")
            }
        }
    } finally {
        invalidDir.deleteRecursively()
    }

    // 4. Phase 009: Info Command Execution
    val oldOut = System.out
    val baos = ByteArrayOutputStream()
    try {
        System.setOut(PrintStream(baos))
        val exitCode = InfoCommand.execute(helloDir)
        check("InfoCommand returns exit code 0", exitCode == 0)
    } finally {
        System.setOut(oldOut)
    }

    val infoOutput = baos.toString()
    check("Info output contains project name", infoOutput.contains("Name:            hello"))
    check("Info output contains application ID", infoOutput.contains("Application ID:  com.example.hello"))
    check("Info output contains Target SDK", infoOutput.contains("Target SDK:      36"))

    println("--------------------------------------------------")
    println("Summary: $passed PASSED, $failed FAILED")
    println("==================================================")

    if (failed > 0) System.exit(1) else println("RESULT: PASS")
}

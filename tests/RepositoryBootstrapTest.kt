package tests

import java.io.File

/**
 * Phase 001 Verification Test: Monorepo Skeleton Verification.
 * Ensures that all directories, documentation, configuration files,
 * and project structures defined in Section 7 of the roadmap exist.
 */
fun main(args: Array<String>) {
    println("==================================================")
    println(" Phase 001: Monorepo Bootstrap Verification Test")
    println("==================================================")

    val root = if (args.isNotEmpty()) File(args[0]) else File(".").canonicalFile
    println("Validating repository root: ${root.absolutePath}")

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

    // 1. Root configuration files
    check("kui.toml exists", File(root, "kui.toml").isFile)
    check("README.md exists", File(root, "README.md").isFile)
    check("LICENSE exists", File(root, "LICENSE").isFile)
    check(".gitignore exists", File(root, ".gitignore").isFile)
    check("Roadmap document exists", File(root, "UI4_KUI_PURE_KOTLIN_PLATFORM_ROADMAP.md").isFile)

    // 2. Documentation structure
    val requiredDocs = listOf(
        "vision.md",
        "architecture.md",
        "ui-api.md",
        "project-format.md",
        "compiler.md",
        "dex.md",
        "packaging.md",
        "accessibility.md",
        "performance.md",
        "testing.md",
        "decisions/0001-pure-kotlin-toolchain.md"
    )
    for (doc in requiredDocs) {
        val f = File(root, "docs/$doc")
        check("docs/$doc exists", f.isFile, "File not found at ${f.absolutePath}")
    }

    // 3. Platform UI4 directories
    val ui4Dirs = listOf(
        "api", "core", "tree", "state", "layout", "render",
        "text", "input", "gesture", "animation", "navigation",
        "accessibility", "theme", "resources", "platform/android"
    )
    for (dir in ui4Dirs) {
        val d = File(root, "platform/ui4/$dir")
        check("platform/ui4/$dir directory exists", d.isDirectory)
    }

    // 4. Platform KUI directories
    val kuiDirs = listOf(
        "cli", "project", "config", "build", "cache",
        "compiler", "classfile", "dex", "axml", "resources",
        "apk", "signing", "device", "testing", "profiling"
    )
    for (dir in kuiDirs) {
        val d = File(root, "platform/kui/$dir")
        check("platform/kui/$dir directory exists", d.isDirectory)
    }

    // 5. Example and Top-Level directories
    check("examples/hello/kui.toml exists", File(root, "examples/hello/kui.toml").isFile)
    check("examples/hello/src/main.kt exists", File(root, "examples/hello/src/main.kt").isFile)
    check("examples/hello/tests/AppTest.kt exists", File(root, "examples/hello/tests/AppTest.kt").isFile)
    check("examples/hello/assets directory exists", File(root, "examples/hello/assets").isDirectory)
    check("benchmarks directory exists", File(root, "benchmarks").isDirectory)
    check("scripts directory exists", File(root, "scripts").isDirectory)

    println("--------------------------------------------------")
    println("Summary: $passed PASSED, $failed FAILED")
    println("==================================================")

    if (failed > 0) {
        System.exit(1)
    } else {
        println("RESULT: PASS")
    }
}

package tests

import kui.cli.KuiVersion
import kui.cli.Version
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

/**
 * Phase 002 Verification Test: KUI Version.
 * Validates version constants and CLI version reporting.
 */
fun main(args: Array<String>) {
    println("==================================================")
    println(" Phase 002: KUI Version Verification Test")
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

    // 1. Version Constant Tests
    check("KuiVersion.CURRENT equals 0.1.0",
        KuiVersion.CURRENT == Version(0, 1, 0),
        "Expected Version(0, 1, 0), got ${KuiVersion.CURRENT}")

    check("KuiVersion.VERSION_STRING is '0.1.0'",
        KuiVersion.VERSION_STRING == "0.1.0",
        "Expected '0.1.0', got '${KuiVersion.VERSION_STRING}'")

    check("KuiVersion.DISPLAY_NAME is 'kui version 0.1.0'",
        KuiVersion.DISPLAY_NAME == "kui version 0.1.0",
        "Expected 'kui version 0.1.0', got '${KuiVersion.DISPLAY_NAME}'")

    // 2. Cross-check against root kui.toml
    val kuiToml = File(root, "kui.toml")
    if (kuiToml.isFile) {
        val lines = kuiToml.readLines()
        val versionLine = lines.firstOrNull { it.trim().startsWith("version") }
        val tomlVersion = versionLine?.substringAfter("=")?.trim()?.trim('"', '\'')
        check("kui.toml version matches KuiVersion.VERSION_STRING",
            tomlVersion == KuiVersion.VERSION_STRING,
            "kui.toml has '$tomlVersion' but KuiVersion has '${KuiVersion.VERSION_STRING}'")
    } else {
        check("kui.toml present for version check", false, "kui.toml not found at ${kuiToml.absolutePath}")
    }

    // 3. CLI Output Tests
    fun captureCliOutput(cliArgs: Array<String>): String {
        val oldOut = System.out
        val baos = ByteArrayOutputStream()
        try {
            System.setOut(PrintStream(baos))
            kui.cli.main(cliArgs)
        } finally {
            System.setOut(oldOut)
        }
        return baos.toString().trim()
    }

    val outDoubleDash = captureCliOutput(arrayOf("--version"))
    check("kui --version output matches DISPLAY_NAME",
        outDoubleDash == KuiVersion.DISPLAY_NAME,
        "Got: '$outDoubleDash'")

    val outSingleDash = captureCliOutput(arrayOf("-v"))
    check("kui -v output matches DISPLAY_NAME",
        outSingleDash == KuiVersion.DISPLAY_NAME,
        "Got: '$outSingleDash'")

    val outSubcommand = captureCliOutput(arrayOf("version"))
    check("kui version output matches DISPLAY_NAME",
        outSubcommand == KuiVersion.DISPLAY_NAME,
        "Got: '$outSubcommand'")

    println("--------------------------------------------------")
    println("Summary: $passed PASSED, $failed FAILED")
    println("==================================================")

    if (failed > 0) {
        System.exit(1)
    } else {
        println("RESULT: PASS")
    }
}

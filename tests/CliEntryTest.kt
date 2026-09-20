package tests

import kui.cli.Help
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

/**
 * Phase 003 Verification Test: CLI Entry.
 * Validates minimal CLI entry point and help display when run without arguments.
 */
fun main(args: Array<String>) {
    println("==================================================")
    println(" Phase 003: CLI Entry Verification Test")
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

    // 1. Running without args shows Help.TEXT
    val emptyArgsOutput = captureCliOutput(emptyArray())
    check("Running without args produces non-empty output", emptyArgsOutput.isNotEmpty())
    check("Running without args matches Help.TEXT exactly",
        emptyArgsOutput == Help.TEXT.trim(),
        "Output differed from Help.TEXT")

    // 2. Contains essential CLI commands from Section 9 of roadmap
    val requiredCommands = listOf(
        "new <name>", "doctor", "info", "build", "run",
        "install", "launch", "test", "clean", "bench",
        "profile", "ui-tree", "help"
    )
    for (cmd in requiredCommands) {
        check("Help contains command '$cmd'",
            emptyArgsOutput.contains(cmd),
            "Command '$cmd' not found in help output")
    }

    // 3. Contains standard flags
    check("Help contains --version flag", emptyArgsOutput.contains("-v, --version"))
    check("Help contains --help flag", emptyArgsOutput.contains("-h, --help"))

    // 4. Parity with explicit help calls
    val dashHelp = captureCliOutput(arrayOf("--help"))
    val shortHelp = captureCliOutput(arrayOf("-h"))
    val commandHelp = captureCliOutput(arrayOf("help"))

    check("kui --help equals no-arg output", dashHelp == emptyArgsOutput)
    check("kui -h equals no-arg output", shortHelp == emptyArgsOutput)
    check("kui help equals no-arg output", commandHelp == emptyArgsOutput)

    println("--------------------------------------------------")
    println("Summary: $passed PASSED, $failed FAILED")
    println("==================================================")

    if (failed > 0) {
        System.exit(1)
    } else {
        println("RESULT: PASS")
    }
}

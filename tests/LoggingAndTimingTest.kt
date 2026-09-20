package tests

import kui.build.KuiLogger
import kui.build.LogLevel
import kui.build.Stopwatch
import java.io.ByteArrayOutputStream
import java.io.PrintStream

/**
 * Verification test for Phase 010 (Logging) and Phase 011 (Timing).
 */
fun main() {
    println("==================================================")
    println(" Phases 010 & 011: Logging & Timing Tests")
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

    // 1. Phase 010: Logging Filtering
    val outStream = ByteArrayOutputStream()
    val errStream = ByteArrayOutputStream()
    val logger = KuiLogger(
        level = LogLevel.WARN,
        out = PrintStream(outStream),
        err = PrintStream(errStream)
    )

    logger.debug("hidden debug message")
    logger.info("hidden info message")
    logger.warn("visible warning")
    logger.error("visible error")

    val outText = outStream.toString()
    val errText = errStream.toString()

    check("Debug message is filtered out at WARN level", !outText.contains("hidden debug message"))
    check("Info message is filtered out at WARN level", !outText.contains("hidden info message"))
    check("Warning message is logged", errText.contains("[WARN] visible warning"))
    check("Error message is logged", errText.contains("[ERROR] visible error"))

    // 2. Phase 011: Stopwatch Monotonic Timing
    val sw = Stopwatch.start()
    val t1 = sw.elapsedNanos()
    Thread.sleep(20)
    val t2 = sw.elapsedNanos()

    check("Stopwatch is monotonic (t2 >= t1)", t2 >= t1)
    check("Elapsed millis >= 15ms after 20ms sleep", sw.elapsedMillis() >= 15)
    check("Formatted timing string non-empty", sw.format().isNotEmpty())

    val (result, measuredMs) = Stopwatch.measure {
        Thread.sleep(10)
        42
    }
    check("Stopwatch.measure returns block result", result == 42)
    check("Stopwatch.measure records elapsed time", measuredMs >= 8)

    println("--------------------------------------------------")
    println("Summary: $passed PASSED, $failed FAILED")
    println("==================================================")

    if (failed > 0) System.exit(1) else println("RESULT: PASS")
}

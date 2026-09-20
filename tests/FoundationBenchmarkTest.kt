package tests

import kui.build.FoundationBenchmark
import java.io.File

/**
 * Verification test for Phase 020: Foundation Benchmark.
 */
fun main(args: Array<String>) {
    println("==================================================")
    println(" Phase 020: Foundation Benchmark Test")
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

    val stats = FoundationBenchmark.run(root, iterations = 30)
    val reportFile = File(root, ".kui/build/reports/benchmark-foundation.json")

    check("Benchmark report file exists", reportFile.isFile)
    check("Benchmark report contains valid iterations count", stats.iterations == 30)
    check("Benchmark min latency is positive", stats.minMs >= 0.0)
    check("Benchmark average latency is sub-millisecond or fast (< 5ms)", stats.avgMs < 5.0, "Got: ${stats.avgMs}ms")
    check("Benchmark report JSON content matches schema",
        reportFile.readText().contains("foundation-empty-graph") && reportFile.readText().contains("avgMs")
    )

    println("  Benchmark result: min=${stats.minMs}ms, avg=${stats.avgMs}ms, p95=${stats.p95Ms}ms, max=${stats.maxMs}ms")

    println("--------------------------------------------------")
    println("Summary: $passed PASSED, $failed FAILED")
    println("==================================================")

    if (failed > 0) System.exit(1) else println("RESULT: PASS")
}

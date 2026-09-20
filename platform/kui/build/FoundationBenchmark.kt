package kui.build

import kui.cache.CacheDirectories
import java.io.File

data class BenchmarkStats(
    val iterations: Int,
    val minMs: Double,
    val maxMs: Double,
    val avgMs: Double,
    val p95Ms: Double
)

/**
 * Foundation Task Graph Benchmark (Phase 020).
 * Measures empty graph and baseline task execution latency.
 */
object FoundationBenchmark {
    fun run(projectRoot: File, iterations: Int = 50): BenchmarkStats {
        val buildDirs = BuildDirectories(projectRoot)
        val cacheDirs = CacheDirectories(projectRoot)
        buildDirs.ensureCreated()
        cacheDirs.ensureCreated()

        val logger = KuiLogger()
        val context = TaskExecutionContext(projectRoot, buildDirs, logger)
        val taskCache = TaskCache(cacheDirs)

        // Warmup
        val warmupGraph = TaskGraph()
        warmupGraph.register(ActionTask("noop1", "No-op task 1") { TaskResult(TaskStatus.SUCCESS, 0) })
        warmupGraph.register(ActionTask("noop2", "No-op task 2", dependencies = listOf("noop1")) { TaskResult(TaskStatus.SUCCESS, 0) })
        repeat(5) {
            val order = warmupGraph.resolveExecutionOrder("noop2")
            for (t in order) taskCache.executeWithCache(t, context)
        }

        // Measurement
        val timingsNanos = LongArray(iterations)

        for (i in 0 until iterations) {
            val start = System.nanoTime()
            val graph = TaskGraph()
            graph.register(ActionTask("stepA", "Step A") { TaskResult(TaskStatus.SUCCESS, 0) })
            graph.register(ActionTask("stepB", "Step B", dependencies = listOf("stepA")) { TaskResult(TaskStatus.SUCCESS, 0) })
            graph.register(ActionTask("stepC", "Step C", dependencies = listOf("stepB")) { TaskResult(TaskStatus.SUCCESS, 0) })

            val order = graph.resolveExecutionOrder("stepC")
            for (task in order) {
                taskCache.executeWithCache(task, context)
            }
            timingsNanos[i] = System.nanoTime() - start
        }

        timingsNanos.sort()
        val timingsMs = timingsNanos.map { it / 1_000_000.0 }
        val minMs = timingsMs.first()
        val maxMs = timingsMs.last()
        val avgMs = timingsMs.average()
        val p95Index = ((iterations * 0.95).toInt()).coerceAtMost(iterations - 1)
        val p95Ms = timingsMs[p95Index]

        val stats = BenchmarkStats(iterations, minMs, maxMs, avgMs, p95Ms)

        // Write report
        val reportFile = File(buildDirs.reportsDir, "benchmark-foundation.json")
        val json = buildString {
            appendLine("{")
            appendLine("  \"benchmark\": \"foundation-empty-graph\",")
            appendLine("  \"iterations\": ${stats.iterations},")
            appendLine("  \"minMs\": ${"%.3f".format(java.util.Locale.US, stats.minMs)},")
            appendLine("  \"maxMs\": ${"%.3f".format(java.util.Locale.US, stats.maxMs)},")
            appendLine("  \"avgMs\": ${"%.3f".format(java.util.Locale.US, stats.avgMs)},")
            appendLine("  \"p95Ms\": ${"%.3f".format(java.util.Locale.US, stats.p95Ms)}")
            appendLine("}")
        }
        reportFile.writeText(json)

        return stats
    }
}

package ui4.render

import ui4.tree.UiRoot
import ui4.tree.markDirty

/**
 * Benchmark runner for UI4 rendering engine performance (Phase 100).
 */
object RenderBenchmark {

    data class BenchmarkReport(
        val iterations: Int,
        val minNanos: Long,
        val avgNanos: Long,
        val p95Nanos: Long,
        val maxNanos: Long,
        val opCount: Int
    ) {
        val minMs: Double get() = minNanos / 1_000_000.0
        val avgMs: Double get() = avgNanos / 1_000_000.0
        val p95Ms: Double get() = p95Nanos / 1_000_000.0
        val maxMs: Double get() = maxNanos / 1_000_000.0

        val meets60FpsBudget: Boolean get() = p95Ms < 16.67
    }

    /**
     * Executes repeated render passes measuring draw latency.
     */
    fun run(root: UiRoot, iterations: Int = 100): BenchmarkReport {
        require(iterations > 0) { "iterations must be > 0" }
        val canvas = RecordingCanvas()
        val pipeline = RenderPipeline()

        // Warm up pass
        root.markDirty()
        pipeline.render(root, canvas, force = true)

        val samples = LongArray(iterations)
        var lastOpCount = 0

        for (i in 0 until iterations) {
            canvas.clear()
            root.markDirty()
            val result = pipeline.render(root, canvas, force = true)
            samples[i] = result.durationNanos
            lastOpCount = result.opCount
        }

        samples.sort()
        val min = samples.first()
        val max = samples.last()
        val avg = samples.average().toLong()
        val p95Index = ((iterations * 0.95).toInt()).coerceIn(0, iterations - 1)
        val p95 = samples[p95Index]

        return BenchmarkReport(
            iterations = iterations,
            minNanos = min,
            avgNanos = avg,
            p95Nanos = p95,
            maxNanos = max,
            opCount = lastOpCount
        )
    }
}

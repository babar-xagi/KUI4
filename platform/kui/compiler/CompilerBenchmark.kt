package kui.compiler

import kui.build.BuildDirectories
import kui.build.CleanCommand
import kui.build.Stopwatch
import kui.cache.CacheDirectories
import kui.project.ProjectFinder
import java.io.File
import java.util.Locale

/**
 * Benchmark metrics for compiler operations (Phase 055).
 */
data class CompilerBenchmarkStats(
    val coldCompileMs: Long,
    val warmCompileMs: Long,
    val iterations: Int,
    val avgWarmMs: Double,
    val minWarmMs: Long,
    val maxWarmMs: Long
)

/**
 * Runs reproducible compilation benchmarks against the Hello application (Phase 055).
 */
object CompilerBenchmark {

    fun run(projectRoot: File, warmIterations: Int = 3): CompilerBenchmarkStats {
        val srcDir = File(projectRoot, "src")
        val buildDirs = BuildDirectories(projectRoot)
        val cacheDirs = CacheDirectories(projectRoot)

        buildDirs.ensureCreated()
        cacheDirs.ensureCreated()

        val sources = SourceDiscovery.findSources(srcDir)
        val kotlinc = CompilerDiscovery.findKotlinc()

        // 1. Measure Cold Compilation (clean output first)
        buildDirs.clean()
        buildDirs.ensureCreated()

        val ui4Jar = UI4Bootstrap.ensureApiJar(buildDirs.buildDir, kotlinc.path)
        val classpath = ClasspathModel()
        classpath.add(ui4Jar)

        val coldTimer = Stopwatch.start()
        val coldResult = KotlinCompiler.compile(
            sources = sources,
            outputDir = buildDirs.classesDir,
            classpath = classpath,
            kotlincPath = kotlinc.path,
            jvmTarget = "21"
        )
        val coldMs = coldTimer.elapsedMillis()

        if (!coldResult.isSuccess) {
            throw IllegalStateException("Benchmark cold compilation failed:\n${coldResult.rawOutput}")
        }

        // 2. Measure Warm Compilation iterations
        val warmTimes = mutableListOf<Long>()
        for (i in 1..warmIterations) {
            val warmTimer = Stopwatch.start()
            val warmResult = KotlinCompiler.compile(
                sources = sources,
                outputDir = buildDirs.classesDir,
                classpath = classpath,
                kotlincPath = kotlinc.path,
                jvmTarget = "21"
            )
            warmTimes.add(warmTimer.elapsedMillis())
            if (!warmResult.isSuccess) {
                throw IllegalStateException("Benchmark warm compilation iteration $i failed")
            }
        }

        val avgWarm = warmTimes.average()
        val minWarm = warmTimes.minOrNull() ?: 0L
        val maxWarm = warmTimes.maxOrNull() ?: 0L

        val stats = CompilerBenchmarkStats(
            coldCompileMs = coldMs,
            warmCompileMs = warmTimes.firstOrNull() ?: 0L,
            iterations = warmIterations,
            avgWarmMs = avgWarm,
            minWarmMs = minWarm,
            maxWarmMs = maxWarm
        )

        // Save report JSON
        saveReport(buildDirs.reportsDir, stats)
        return stats
    }

    private fun saveReport(reportsDir: File, stats: CompilerBenchmarkStats) {
        reportsDir.mkdirs()
        val reportFile = File(reportsDir, "benchmark-compiler.json")
        val json = """
{
  "benchmark": "compiler-baseline",
  "cold_compile_ms": ${stats.coldCompileMs},
  "warm_compile_ms": ${stats.warmCompileMs},
  "iterations": ${stats.iterations},
  "avg_warm_ms": ${String.format(Locale.US, "%.2f", stats.avgWarmMs)},
  "min_warm_ms": ${stats.minWarmMs},
  "max_warm_ms": ${stats.maxWarmMs},
  "timestamp": ${System.currentTimeMillis()}
}
""".trimIndent()
        reportFile.writeText(json)
    }
}

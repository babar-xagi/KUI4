package tests

import kui.build.ActionTask
import kui.build.BuildDirectories
import kui.build.BuildReport
import kui.build.KuiLogger
import kui.build.TaskCache
import kui.build.TaskExecutionContext
import kui.build.TaskExecutionRecord
import kui.build.TaskGraph
import kui.build.TaskResult
import kui.build.TaskStatus
import kui.cache.CacheDirectories
import java.io.File

/**
 * Verification test for:
 * - Phase 016: Task Abstraction
 * - Phase 017: Task Dependencies (DAG, topological sort, cycle detection)
 * - Phase 018: Task Caching (skip unchanged, second run cached)
 * - Phase 019: Build Report (includes timings and structured JSON)
 */
fun main() {
    println("==================================================")
    println(" Phases 016-019: Task Graph, Cache & Report Tests")
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

    val tempProject = File(System.getProperty("java.io.tmpdir"), "kui_task_test_" + System.currentTimeMillis())
    tempProject.mkdirs()

    try {
        val buildDirs = BuildDirectories(tempProject).apply { ensureCreated() }
        val cacheDirs = CacheDirectories(tempProject).apply { ensureCreated() }
        val logger = KuiLogger()
        val context = TaskExecutionContext(tempProject, buildDirs, logger)
        val taskCache = TaskCache(cacheDirs)

        // 1. Phase 016 & 017: Task Dependencies & Execution Order
        val executionOrder = mutableListOf<String>()

        val taskCompile = ActionTask("compile", "Compiles source") {
            executionOrder.add("compile")
            TaskResult(TaskStatus.SUCCESS, 10)
        }
        val taskDex = ActionTask("dex", "Generates DEX", dependencies = listOf("compile")) {
            executionOrder.add("dex")
            TaskResult(TaskStatus.SUCCESS, 15)
        }
        val taskPackage = ActionTask("package", "Packages APK", dependencies = listOf("dex")) {
            executionOrder.add("package")
            TaskResult(TaskStatus.SUCCESS, 20)
        }

        val graph = TaskGraph()
            .register(taskCompile)
            .register(taskDex)
            .register(taskPackage)

        val resolved = graph.resolveExecutionOrder("package")
        val orderNames = resolved.map { it.name }
        check("Topological sort produces correct dependency order", orderNames == listOf("compile", "dex", "package"))

        // Cycle detection test
        val cyclicGraph = TaskGraph()
            .register(ActionTask("A", "Task A", dependencies = listOf("B")) { TaskResult(TaskStatus.SUCCESS, 0) })
            .register(ActionTask("B", "Task B", dependencies = listOf("A")) { TaskResult(TaskStatus.SUCCESS, 0) })

        var cycleDetected = false
        try {
            cyclicGraph.resolveExecutionOrder("A")
        } catch (e: IllegalStateException) {
            cycleDetected = e.message?.contains("Circular") == true
        }
        check("Circular dependency correctly detected and rejected", cycleDetected)

        // 2. Phase 018: Task Caching (Skip unchanged on second run)
        val inputFile = File(tempProject, "input.txt").apply { writeText("original input") }
        val outputFile = File(buildDirs.buildDir, "output.txt")
        var executionCount = 0

        val cachedTask = ActionTask(
            name = "fileProcessor",
            description = "Processes input file",
            inputs = listOf(inputFile),
            outputs = listOf(outputFile)
        ) {
            executionCount++
            outputFile.writeText("processed: " + inputFile.readText())
            TaskResult(TaskStatus.SUCCESS, 25)
        }

        // First run: executes
        val run1 = taskCache.executeWithCache(cachedTask, context)
        check("Run 1 status is SUCCESS", run1.status == TaskStatus.SUCCESS)
        check("Run 1 executed action (count = 1)", executionCount == 1)
        check("Run 1 produced output file", outputFile.isFile)

        // Second run: cached
        val run2 = taskCache.executeWithCache(cachedTask, context)
        check("Run 2 status is SKIPPED_CACHED", run2.status == TaskStatus.SKIPPED_CACHED)
        check("Run 2 did not re-execute action (count = 1)", executionCount == 1)
        check("Run 2 duration is 0ms", run2.durationMs == 0L)

        // Modify input: invalidates cache
        inputFile.writeText("modified input")
        val run3 = taskCache.executeWithCache(cachedTask, context)
        check("Run 3 after input mutation is SUCCESS", run3.status == TaskStatus.SUCCESS)
        check("Run 3 executed action (count = 2)", executionCount == 2)

        // 3. Phase 019: Build Report Generation
        val records = listOf(
            TaskExecutionRecord("compile", TaskStatus.SUCCESS, 120),
            TaskExecutionRecord("dex", TaskStatus.SKIPPED_CACHED, 0),
            TaskExecutionRecord("package", TaskStatus.SUCCESS, 45)
        )
        val report = BuildReport(totalDurationMs = 165, tasks = records)
        val reportFile = File(buildDirs.reportsDir, "test-build-report.json")
        report.writeJson(reportFile)

        check("Build report file created", reportFile.isFile)
        val reportJson = reportFile.readText()
        check("Build report JSON contains totalDurationMs", reportJson.contains("\"totalDurationMs\": 165"))
        check("Build report JSON contains task names", reportJson.contains("\"compile\"") && reportJson.contains("\"dex\""))
        check("Build report summary formats cleanly", report.formatSummary().contains("1 executed, 1 up-to-date") || report.formatSummary().contains("Total Duration: 165ms"))

    } finally {
        tempProject.deleteRecursively()
    }

    println("--------------------------------------------------")
    println("Summary: $passed PASSED, $failed FAILED")
    println("==================================================")

    if (failed > 0) System.exit(1) else println("RESULT: PASS")
}

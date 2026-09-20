package kui.build

import java.io.File

data class TaskExecutionRecord(
    val taskName: String,
    val status: TaskStatus,
    val durationMs: Long,
    val message: String? = null
)

/**
 * Build execution report generator (Phase 019).
 */
data class BuildReport(
    val totalDurationMs: Long,
    val tasks: List<TaskExecutionRecord>
) {
    fun writeJson(file: File) {
        file.parentFile?.mkdirs()
        val json = buildString {
            appendLine("{")
            appendLine("  \"totalDurationMs\": $totalDurationMs,")
            appendLine("  \"tasks\": [")
            tasks.forEachIndexed { index, record ->
                val comma = if (index < tasks.size - 1) "," else ""
                appendLine("    {")
                appendLine("      \"name\": \"${record.taskName}\",")
                appendLine("      \"status\": \"${record.status}\",")
                appendLine("      \"durationMs\": ${record.durationMs},")
                appendLine("      \"message\": ${if (record.message != null) "\"${record.message}\"" else "null"}")
                appendLine("    }$comma")
            }
            appendLine("  ]")
            appendLine("}")
        }
        file.writeText(json)
    }

    fun formatSummary(): String = buildString {
        val executed = tasks.count { it.status == TaskStatus.SUCCESS }
        val cached = tasks.count { it.status == TaskStatus.SKIPPED_CACHED }
        val failed = tasks.count { it.status == TaskStatus.FAILED }

        appendLine("Build Summary:")
        for (t in tasks) {
            val statusLabel = when (t.status) {
                TaskStatus.SUCCESS -> "[EXECUTED]"
                TaskStatus.SKIPPED_CACHED -> "[UP-TO-DATE]"
                TaskStatus.FAILED -> "[FAILED]"
            }
            val paddedName = t.taskName.padEnd(20)
            appendLine("  $statusLabel $paddedName ${t.durationMs}ms")
        }
        appendLine("Total Duration: ${totalDurationMs}ms ($executed executed, $cached up-to-date, $failed failed)")
    }
}

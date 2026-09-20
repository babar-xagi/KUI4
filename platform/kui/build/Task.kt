package kui.build

import java.io.File

enum class TaskStatus {
    SUCCESS,
    SKIPPED_CACHED,
    FAILED
}

data class TaskResult(
    val status: TaskStatus,
    val durationMs: Long,
    val message: String? = null
)

class TaskExecutionContext(
    val projectRoot: File,
    val buildDirs: BuildDirectories,
    val logger: KuiLogger
)

/**
 * Fundamental Task abstraction for the KUI build graph (Phase 016).
 */
interface Task {
    val name: String
    val description: String
    val dependencies: List<String> get() = emptyList()
    val inputs: List<File> get() = emptyList()
    val outputs: List<File> get() = emptyList()

    fun execute(context: TaskExecutionContext): TaskResult
}

/**
 * Functional Task implementation for ad-hoc and test tasks.
 */
class ActionTask(
    override val name: String,
    override val description: String,
    override val dependencies: List<String> = emptyList(),
    override val inputs: List<File> = emptyList(),
    override val outputs: List<File> = emptyList(),
    private val action: (TaskExecutionContext) -> TaskResult
) : Task {
    override fun execute(context: TaskExecutionContext): TaskResult = action(context)
}

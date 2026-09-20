package kui.build

import kui.cache.CacheDirectories
import kui.cache.DirectoryHasher
import kui.cache.Hasher
import java.io.File

/**
 * Fingerprint and cache engine for task execution (Phase 018).
 */
class TaskCache(val cacheDirs: CacheDirectories) {

    fun computeFingerprint(task: Task): String {
        val inputHashes = buildString {
            append(task.name)
            append(":")
            for (input in task.inputs) {
                if (input.isFile) {
                    append(input.name)
                    append("=")
                    append(Hasher.sha256(input))
                    append(";")
                } else if (input.isDirectory) {
                    append(input.name)
                    append("=")
                    append(DirectoryHasher.hashDirectory(input))
                    append(";")
                }
            }
            for (output in task.outputs) {
                append("out:")
                append(output.name)
                append(";")
            }
        }
        return Hasher.sha256(inputHashes)
    }

    fun executeWithCache(task: Task, context: TaskExecutionContext): TaskResult {
        // If task declares no inputs and outputs, run directly without caching
        if (task.inputs.isEmpty() && task.outputs.isEmpty()) {
            val sw = Stopwatch.start()
            val result = task.execute(context)
            return result.copy(durationMs = sw.elapsedMillis())
        }

        val fingerprint = computeFingerprint(task)
        val cacheFile = cacheDirs.getTaskCacheFile(fingerprint)

        // Check if cached
        if (cacheFile.isFile) {
            val allOutputsExist = task.outputs.all { it.exists() }
            if (allOutputsExist) {
                return TaskResult(
                    status = TaskStatus.SKIPPED_CACHED,
                    durationMs = 0,
                    message = "UP-TO-DATE"
                )
            }
        }

        // Execute task
        val sw = Stopwatch.start()
        val result = task.execute(context)
        val elapsed = sw.elapsedMillis()

        if (result.status == TaskStatus.SUCCESS) {
            // Write cache metadata
            val metadata = buildString {
                appendLine("task=${task.name}")
                appendLine("fingerprint=$fingerprint")
                appendLine("timestamp=${System.currentTimeMillis()}")
            }
            cacheFile.writeText(metadata)
        }

        return result.copy(durationMs = elapsed)
    }
}

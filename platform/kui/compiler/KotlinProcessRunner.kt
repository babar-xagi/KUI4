package kui.compiler

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * Result of an external process execution.
 */
data class ProcessResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val durationMs: Long,
    val commandLine: List<String>
) {
    val isSuccess: Boolean get() = exitCode == 0
}

/**
 * Wrapper for invoking external compilers and tools with robust I/O handling and timing (Phase 039).
 */
object KotlinProcessRunner {

    private val isWindows =
        System.getProperty("os.name")?.lowercase(Locale.US)?.contains("windows") == true

    /**
     * Executes an external process synchronously, capturing stdout, stderr, exit code, and execution time.
     */
    fun run(
        command: List<String>,
        workingDir: File? = null,
        environment: Map<String, String> = emptyMap()
    ): ProcessResult {
        require(command.isNotEmpty()) { "Command line cannot be empty." }

        val finalCommand = if (isWindows && command[0].endsWith(".bat", ignoreCase = true) ||
            command[0].endsWith(".cmd", ignoreCase = true)
        ) {
            listOf("cmd.exe", "/c") + command
        } else {
            command
        }

        val startTime = System.nanoTime()
        val processBuilder = ProcessBuilder(finalCommand)

        if (workingDir != null) {
            processBuilder.directory(workingDir)
        }

        if (environment.isNotEmpty()) {
            processBuilder.environment().putAll(environment)
        }

        processBuilder.redirectErrorStream(false)

        val process = processBuilder.start()
        val executor = Executors.newFixedThreadPool(2)

        var stdoutFuture: Future<String>? = null
        var stderrFuture: Future<String>? = null

        try {
            stdoutFuture = executor.submit<String> {
                BufferedReader(InputStreamReader(process.inputStream, StandardCharsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            }

            stderrFuture = executor.submit<String> {
                BufferedReader(InputStreamReader(process.errorStream, StandardCharsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            }

            val exitCode = process.waitFor()
            val stdout = stdoutFuture.get()
            val stderr = stderrFuture.get()
            val durationMs = (System.nanoTime() - startTime) / 1_000_000

            return ProcessResult(
                exitCode = exitCode,
                stdout = stdout,
                stderr = stderr,
                durationMs = durationMs,
                commandLine = finalCommand
            )
        } finally {
            executor.shutdown()
        }
    }
}

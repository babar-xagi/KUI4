package kui.compiler

import java.io.File

/**
 * Result of a Kotlin compilation execution.
 */
data class CompilationResult(
    val isSuccess: Boolean,
    val durationMs: Long,
    val diagnostics: List<CompilerDiagnostic>,
    val rawOutput: String,
    val commandLine: List<String>,
    val outputDir: File
)

/**
 * Direct invoker for the official Kotlin compiler (Phases 039, 040, 041, 043).
 */
object KotlinCompiler {

    /**
     * Compiles the given source files into outputDir.
     */
    fun compile(
        sources: List<File>,
        outputDir: File,
        classpath: ClasspathModel = ClasspathModel(),
        kotlincPath: String = CompilerDiscovery.findKotlinc().path,
        jvmTarget: String = "21",
        verbose: Boolean = false,
        workingDir: File? = null
    ): CompilationResult {
        require(sources.isNotEmpty()) { "No Kotlin sources provided for compilation." }

        outputDir.mkdirs()

        val command = mutableListOf<String>()
        command.add(kotlincPath)

        // cmd.exe has an 8191-character limit. Keep source paths in a quoted UTF-8 argument file.
        val sourcesFile = File.createTempFile("kui-sources-", ".txt", outputDir.parentFile)
        sourcesFile.writeText(sources.joinToString("\n") {
            "\"${it.absolutePath.replace('\\', '/').replace("\"", "\\\"")}\""
        })
        command.add("@${sourcesFile.absolutePath}")

        command.add("-d")
        command.add(outputDir.absolutePath)

        if (!classpath.isEmpty()) {
            command.add("-cp")
            command.add(classpath.asClasspathString())
        }

        command.add("-jvm-target")
        command.add(jvmTarget)

        if (verbose) {
            command.add("-verbose")
        }

        val result = try {
            KotlinProcessRunner.run(command, workingDir = workingDir)
        } finally {
            sourcesFile.delete()
        }
        val combinedOutput = buildString {
            if (result.stdout.isNotBlank()) appendLine(result.stdout.trim())
            if (result.stderr.isNotBlank()) appendLine(result.stderr.trim())
        }.trim()

        val diagnostics = DiagnosticParser.parse(combinedOutput)

        return CompilationResult(
            isSuccess = result.isSuccess,
            durationMs = result.durationMs,
            diagnostics = diagnostics,
            rawOutput = combinedOutput,
            commandLine = result.commandLine,
            outputDir = outputDir
        )
    }
}

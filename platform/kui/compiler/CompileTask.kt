package kui.compiler

import kui.build.BuildDirectories
import kui.build.Task
import kui.build.TaskExecutionContext
import kui.build.TaskResult
import kui.build.TaskStatus
import java.io.File

/**
 * Task graph node that compiles project Kotlin sources (Phase 048, 051, 052, 053).
 */
class CompileTask(
    val sources: List<File>,
    val classpath: ClasspathModel,
    val verbose: Boolean = false,
    val jvmTarget: String = "21",
    val kotlincPath: String? = null,
    val extraInputs: List<File> = emptyList(),
    override val dependencies: List<String> = emptyList()
) : Task {

    override val name: String = "compileKotlin"
    override val description: String = "Compiles Kotlin sources into JVM bytecode"

    override val inputs: List<File>
        get() = sources + classpath.getEntries().map { it.file } + extraInputs

    override val outputs: List<File>
        get() = emptyList() // Target directory evaluated at execution time from context

    override fun execute(context: TaskExecutionContext): TaskResult {
        val outDir = context.buildDirs.classesDir
        outDir.mkdirs()

        val resolvedKotlinc = kotlincPath ?: CompilerDiscovery.findKotlinc().path
        if (resolvedKotlinc == "NOT_FOUND") {
            return TaskResult(
                status = TaskStatus.FAILED,
                durationMs = 0,
                message = "Kotlin compiler (kotlinc) not found in PATH or environment. Run 'kui doctor' to diagnose."
            )
        }

        if (verbose) {
            context.logger.info("Compile inputs: ${sources.size} Kotlin file(s)")
            context.logger.info("Classpath entries: ${classpath.getEntries().size}")
            context.logger.info("Compiler: $resolvedKotlinc")
            context.logger.info("Output: ${outDir.absolutePath}")
        }

        val result = KotlinCompiler.compile(
            sources = sources,
            outputDir = outDir,
            classpath = classpath,
            kotlincPath = resolvedKotlinc,
            jvmTarget = jvmTarget,
            verbose = verbose,
            workingDir = context.projectRoot
        )

        if (verbose && result.commandLine.isNotEmpty()) {
            context.logger.info("Command: ${result.commandLine.joinToString(" ")}")
        }

        if (!result.isSuccess) {
            val conciseErrors = DiagnosticParser.formatConcise(result.diagnostics, context.projectRoot)
            val errorMessage = if (conciseErrors.isNotBlank()) {
                conciseErrors
            } else {
                result.rawOutput.ifBlank { "Compilation failed with exit code." }
            }
            return TaskResult(
                status = TaskStatus.FAILED,
                durationMs = result.durationMs,
                message = errorMessage
            )
        }

        val message = "Compiled ${sources.size} Kotlin file(s) in ${result.durationMs}ms"
        return TaskResult(
            status = TaskStatus.SUCCESS,
            durationMs = result.durationMs,
            message = message
        )
    }
}

package kui.compiler

import kui.cache.Hasher
import java.io.File

/**
 * Bootstraps and compiles the standalone UI4 Declarative API jar (Phase 045).
 */
object UI4Bootstrap {

    private const val EMBEDDED_UI4_SOURCE = """
package ui4

class UI4AppScope {
    fun screen(content: UI4ScreenScope.() -> Unit) {
        val scope = UI4ScreenScope()
        scope.content()
    }
}

class UI4ScreenScope {
    fun center(content: UI4ContainerScope.() -> Unit) {
        val scope = UI4ContainerScope()
        scope.content()
    }

    fun column(
        gap: Int = 0,
        alignment: String = "center",
        content: UI4ContainerScope.() -> Unit
    ) {
        val scope = UI4ContainerScope()
        scope.content()
    }

    fun row(
        gap: Int = 0,
        alignment: String = "center",
        content: UI4ContainerScope.() -> Unit
    ) {
        val scope = UI4ContainerScope()
        scope.content()
    }

    fun box(
        alignment: String = "center",
        content: UI4ContainerScope.() -> Unit
    ) {
        val scope = UI4ContainerScope()
        scope.content()
    }

    fun text(value: String, style: TextStyle = TextStyle.Body) {}
    fun button(label: String, onClick: () -> Unit = {}) {}
}

class UI4ContainerScope {
    fun text(value: String, style: TextStyle = TextStyle.Body) {}
    fun button(label: String, onClick: () -> Unit = {}) {}
    fun column(gap: Int = 0, content: UI4ContainerScope.() -> Unit) { content() }
    fun row(gap: Int = 0, content: UI4ContainerScope.() -> Unit) { content() }
    fun box(content: UI4ContainerScope.() -> Unit) { content() }
}

enum class TextStyle {
    Headline,
    Title,
    Body,
    Caption
}

class State<T>(var value: T)

fun <T> state(initial: T): State<T> = State(initial)

fun app(block: UI4AppScope.() -> Unit) {
    val appScope = UI4AppScope()
    appScope.block()
}
"""

    /**
     * Ensures `ui4-api.jar` is compiled and available in `targetDir`.
     * Returns the compiled File.
     */
    fun ensureApiJar(targetDir: File, kotlincPath: String = "kotlinc"): File {
        targetDir.mkdirs()
        val jarFile = File(targetDir, "ui4-api.jar")
        val hashFile = File(targetDir, "ui4-api.sha256")

        val ui4Dir = resolveUi4Dir()
        val sourceFiles = if (ui4Dir != null && ui4Dir.isDirectory) {
            SourceDiscovery.findSources(ui4Dir)
        } else {
            emptyList()
        }

        val currentHash = if (sourceFiles.isNotEmpty()) {
            SourceHasher.hashSources(sourceFiles, ui4Dir!!)
        } else {
            Hasher.hashString(resolveSourceContent())
        }

        // Fast cache check
        if (jarFile.exists() && hashFile.exists()) {
            val cachedHash = hashFile.readText().trim()
            if (cachedHash == currentHash) {
                return jarFile
            }
        }

        val compileSources = if (sourceFiles.isNotEmpty()) {
            sourceFiles.map { it.absolutePath }
        } else {
            val tempSrc = File(targetDir, "UI4.kt")
            tempSrc.writeText(resolveSourceContent())
            listOf(tempSrc.absolutePath)
        }

        val args = listOf(kotlincPath) + compileSources + listOf(
            "-d", jarFile.absolutePath,
            "-jvm-target", "21"
        )

        val result = KotlinProcessRunner.run(args)
        if (!result.isSuccess || !jarFile.exists()) {
            throw IllegalStateException(
                "Failed to compile UI4 API bootstrap jar:\nSTDOUT: ${result.stdout}\nSTDERR: ${result.stderr}"
            )
        }

        // Update hash record
        hashFile.writeText(currentHash)
        return jarFile
    }

    private fun resolveUi4Dir(): File? {
        var curr: File? = File(".").canonicalFile
        while (curr != null) {
            val candidate = File(curr, "platform/ui4")
            if (candidate.exists() && candidate.isDirectory) {
                return candidate
            }
            curr = curr.parentFile
        }
        return null
    }

    private fun resolveSourceContent(): String {
        var curr: File? = File(".").canonicalFile
        while (curr != null) {
            val candidate = File(curr, "platform/ui4/api/UI4.kt")
            if (candidate.exists() && candidate.isFile) {
                return candidate.readText()
            }
            curr = curr.parentFile
        }
        return EMBEDDED_UI4_SOURCE.trimIndent()
    }
}

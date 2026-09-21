package kui.compiler

import kui.cache.Hasher
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

/**
 * Bootstraps and compiles the standalone UI4 Declarative API jar (Phase 045).
 */
object UI4Bootstrap {

    private const val EMBEDDED_UI4_SOURCE = """
package ui4

data class Color(val argb: Long) {
    constructor(argb: Int) : this((argb.toLong()) and 0xFFFFFFFFL)
    companion object {
        val Transparent = Color(0x00000000L)
        val Black = Color(0xFF000000L)
        val White = Color(0xFFFFFFFFL)
        val Red = Color(0xFFEF4444L)
        val Green = Color(0xFF22C55EL)
        val Blue = Color(0xFF3B82F6L)
        val Yellow = Color(0xFFEAB308L)
        val Gray = Color(0xFF888888L)
        val LightGray = Color(0xFFCCCCCCL)
        val DarkGray = Color(0xFF444444L)
        val Purple = Color(0xFF8B5CF6L)
        val Indigo = Color(0xFF6366F1L)
        val Orange = Color(0xFFF97316L)
        val Teal = Color(0xFF14B8A6L)
        val Pink = Color(0xFFEC4899L)
        fun hex(value: String): Color = Color(0xFF000000L)
    }
}

enum class Alignment {
    TopStart, TopCenter, TopEnd,
    CenterStart, Center, CenterEnd,
    BottomStart, BottomCenter, BottomEnd;
    companion object {
        val TopStart = Alignment.TopStart
        val TopCenter = Alignment.TopCenter
        val TopEnd = Alignment.TopEnd
        val CenterStart = Alignment.CenterStart
        val Center = Alignment.Center
        val CenterEnd = Alignment.CenterEnd
        val BottomStart = Alignment.BottomStart
        val BottomCenter = Alignment.BottomCenter
        val BottomEnd = Alignment.BottomEnd
    }
}

enum class FontWeight(val weight: Int) {
    Thin(100), Light(300), Normal(400), Medium(500), SemiBold(600), Bold(700), ExtraBold(800), Black(900)
}

enum class TextAlign { Start, Center, End, Justify }

data class TextStyle(
    val fontSize: Float = 14f,
    val fontWeight: FontWeight = FontWeight.Normal,
    val color: Color? = null,
    val fontFamily: String = "System",
    val textAlign: TextAlign = TextAlign.Start
) {
    constructor(
        fontSize: Int,
        fontWeight: FontWeight = FontWeight.Normal,
        color: Color? = null,
        fontFamily: String = "System",
        textAlign: TextAlign = TextAlign.Start
    ) : this(fontSize.toFloat(), fontWeight, color, fontFamily, textAlign)

    fun copy(fontSize: Int): TextStyle = copy(fontSize = fontSize.toFloat())

    companion object {
        val Headline = TextStyle(fontSize = 28f, fontWeight = FontWeight.Bold)
        val Title = TextStyle(fontSize = 20f, fontWeight = FontWeight.Medium)
        val Body = TextStyle(fontSize = 14f, fontWeight = FontWeight.Normal)
        val Caption = TextStyle(fontSize = 11f, fontWeight = FontWeight.Normal)
    }
}

interface Modifier {
    companion object : Modifier
}
fun Modifier.padding(all: Float): Modifier = this
fun Modifier.padding(all: Int): Modifier = this
fun Modifier.padding(horizontal: Float = 0f, vertical: Float = 0f): Modifier = this
fun Modifier.padding(horizontal: Int = 0, vertical: Int = 0): Modifier = this
fun Modifier.padding(left: Float = 0f, top: Float = 0f, right: Float = 0f, bottom: Float = 0f): Modifier = this
fun Modifier.padding(left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0): Modifier = this
fun Modifier.background(color: Color): Modifier = this
fun Modifier.backgroundColor(color: Color): Modifier = this
fun Modifier.size(size: Float): Modifier = this
fun Modifier.size(size: Int): Modifier = this
fun Modifier.size(width: Float, height: Float): Modifier = this
fun Modifier.size(width: Int, height: Int): Modifier = this
fun Modifier.width(width: Float): Modifier = this
fun Modifier.width(width: Int): Modifier = this
fun Modifier.height(height: Float): Modifier = this
fun Modifier.height(height: Int): Modifier = this
fun Modifier.fillMaxWidth(fraction: Float = 1.0f): Modifier = this
fun Modifier.fillMaxHeight(fraction: Float = 1.0f): Modifier = this
fun Modifier.fillMaxSize(fraction: Float = 1.0f): Modifier = this
fun Modifier.weight(weight: Float): Modifier = this
fun Modifier.weight(weight: Int): Modifier = this
fun Modifier.alpha(alpha: Float): Modifier = this
fun Modifier.rounded(radius: Float): Modifier = this
fun Modifier.rounded(radius: Int): Modifier = this
fun Modifier.clip(clip: Boolean = true): Modifier = this

interface State<out T> { val value: T }
interface MutableState<T> : State<T> { override var value: T }
class StateImpl<T>(override var value: T) : MutableState<T>
fun <T> mutableStateOf(initial: T): MutableState<T> = StateImpl(initial)
fun <T> state(initial: T): MutableState<T> = StateImpl(initial)
operator fun <T> State<T>.getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): T = value
operator fun <T> MutableState<T>.setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: T) { this.value = value }

class UI4AppScope {
    fun screen(
        modifier: Modifier = Modifier,
        padding: Float = 0f,
        backgroundColor: Color? = null,
        content: UI4ContainerScope.() -> Unit
    ) {
        val scope = UI4ContainerScope()
        scope.content()
    }
    fun screen(
        modifier: Modifier = Modifier,
        padding: Int,
        backgroundColor: Color? = null,
        content: UI4ContainerScope.() -> Unit
    ) = screen(modifier, padding.toFloat(), backgroundColor, content)
    fun screen(content: UI4ContainerScope.() -> Unit) = screen(Modifier, 0f, null, content)
}

class UI4ContainerScope {
    fun center(content: UI4ContainerScope.() -> Unit) { content() }
    fun column(
        gap: Int = 0,
        alignment: Alignment = Alignment.Center,
        modifier: Modifier = Modifier,
        padding: Float = 0f,
        backgroundColor: Color? = null,
        content: UI4ContainerScope.() -> Unit
    ) { content() }
    fun column(
        gap: Int = 0,
        alignment: Alignment = Alignment.Center,
        modifier: Modifier = Modifier,
        padding: Int,
        backgroundColor: Color? = null,
        content: UI4ContainerScope.() -> Unit
    ) = column(gap, alignment, modifier, padding.toFloat(), backgroundColor, content)
    fun column(gap: Int = 0, content: UI4ContainerScope.() -> Unit) = column(gap, Alignment.Center, Modifier, 0f, null, content)

    fun row(
        gap: Int = 0,
        alignment: Alignment = Alignment.Center,
        modifier: Modifier = Modifier,
        padding: Float = 0f,
        backgroundColor: Color? = null,
        content: UI4ContainerScope.() -> Unit
    ) { content() }
    fun row(
        gap: Int = 0,
        alignment: Alignment = Alignment.Center,
        modifier: Modifier = Modifier,
        padding: Int,
        backgroundColor: Color? = null,
        content: UI4ContainerScope.() -> Unit
    ) = row(gap, alignment, modifier, padding.toFloat(), backgroundColor, content)
    fun row(gap: Int = 0, content: UI4ContainerScope.() -> Unit) = row(gap, Alignment.Center, Modifier, 0f, null, content)

    fun box(
        alignment: Alignment = Alignment.Center,
        modifier: Modifier = Modifier,
        padding: Float = 0f,
        backgroundColor: Color? = null,
        content: UI4ContainerScope.() -> Unit
    ) { content() }
    fun box(
        alignment: Alignment = Alignment.Center,
        modifier: Modifier = Modifier,
        padding: Int,
        backgroundColor: Color? = null,
        content: UI4ContainerScope.() -> Unit
    ) = box(alignment, modifier, padding.toFloat(), backgroundColor, content)
    fun box(content: UI4ContainerScope.() -> Unit) = box(Alignment.Center, Modifier, 0f, null, content)

    fun text(
        value: String,
        modifier: Modifier = Modifier,
        style: TextStyle = TextStyle.Body,
        color: Color? = null
    ) {}
    fun text(value: String, style: TextStyle) = text(value, Modifier, style, null)
    fun text(value: String) = text(value, Modifier, TextStyle.Body, null)

    fun button(
        label: String = "",
        text: String = label,
        modifier: Modifier = Modifier,
        backgroundColor: Color? = null,
        textColor: Color? = null,
        enabled: Boolean = true,
        onClick: () -> Unit = {}
    ) {}
}

typealias UI4ScreenScope = UI4ContainerScope

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

        val runningJar = getRunningJarFile()

        // 1. If we have sources on disk, compute their hash and compile
        if (sourceFiles.isNotEmpty()) {
            val currentHash = SourceHasher.hashSources(sourceFiles, ui4Dir!!)
            if (jarFile.exists() && hashFile.exists()) {
                val cachedHash = hashFile.readText().trim()
                if (cachedHash == currentHash) {
                    return jarFile
                }
            }

            // Compile source files with kotlinc to produce complete bytecode and .kotlin_module metadata
            val compileSources = sourceFiles.map { it.absolutePath }
            val args = listOf(kotlincPath) + compileSources + listOf(
                "-d", jarFile.absolutePath,
                "-jvm-target", "21"
            )

            val result = KotlinProcessRunner.run(args)
            if (result.isSuccess && jarFile.exists()) {
                hashFile.writeText(currentHash)
                return jarFile
            }
        }

        // 2. If no sources on disk but running JAR has ui4 classes, extract them
        if (runningJar != null) {
            val jarHash = "jar_${runningJar.lastModified()}_${runningJar.length()}"
            if (jarFile.exists() && hashFile.exists() && hashFile.readText().trim() == jarHash) {
                return jarFile
            }
            if (extractUi4Classes(runningJar, jarFile)) {
                hashFile.writeText(jarHash)
                return jarFile
            }
        }

        // 3. Fallback to embedded source
        val embeddedContent = resolveSourceContent()
        val currentHash = Hasher.hashString(embeddedContent)
        if (jarFile.exists() && hashFile.exists() && hashFile.readText().trim() == currentHash) {
            return jarFile
        }

        val tempSrc = File(targetDir, "UI4.kt")
        tempSrc.writeText(embeddedContent)
        val args = listOf(kotlincPath, tempSrc.absolutePath, "-d", jarFile.absolutePath, "-jvm-target", "21")
        val result = KotlinProcessRunner.run(args)
        if (!result.isSuccess || !jarFile.exists()) {
            throw IllegalStateException(
                "Failed to compile UI4 API bootstrap jar:\nSTDOUT: ${result.stdout}\nSTDERR: ${result.stderr}"
            )
        }

        hashFile.writeText(currentHash)
        return jarFile
    }

    private fun resolveUi4Dir(): File? {
        // 1. Current working dir and its ancestors
        var curr: File? = File(".").canonicalFile
        while (curr != null) {
            val candidate = File(curr, "platform/ui4")
            if (candidate.exists() && candidate.isDirectory) {
                return candidate
            }
            curr = curr.parentFile
        }

        // 2. System property 'kui.home'
        System.getProperty("kui.home")?.let { kuiHome ->
            val candidate = File(kuiHome, "platform/ui4")
            if (candidate.exists() && candidate.isDirectory) return candidate
        }

        // 3. KUI_HOME environment variable
        System.getenv("KUI_HOME")?.let { kuiHome ->
            val candidate = File(kuiHome, "platform/ui4")
            if (candidate.exists() && candidate.isDirectory) return candidate
        }

        // 4. CodeSource location of running JAR / classes
        try {
            val codeSourceLoc = UI4Bootstrap::class.java.protectionDomain?.codeSource?.location
            if (codeSourceLoc != null) {
                var jarDir: File? = File(codeSourceLoc.toURI()).canonicalFile
                if (jarDir?.isFile == true) jarDir = jarDir.parentFile
                while (jarDir != null) {
                    val candidate = File(jarDir, "platform/ui4")
                    if (candidate.exists() && candidate.isDirectory) {
                        return candidate
                    }
                    jarDir = jarDir.parentFile
                }
            }
        } catch (_: Exception) {}

        // 5. Standard installation path
        val defaultInstall = File("C:/Program Files/KUI/platform/ui4")
        if (defaultInstall.exists() && defaultInstall.isDirectory) {
            return defaultInstall
        }

        return null
    }

    private fun resolveSourceContent(): String {
        val ui4Dir = resolveUi4Dir()
        if (ui4Dir != null) {
            val candidate = File(ui4Dir, "api/UI4.kt")
            if (candidate.exists() && candidate.isFile) {
                return candidate.readText()
            }
        }
        return EMBEDDED_UI4_SOURCE.trimIndent()
    }

    private fun getRunningJarFile(): File? {
        return try {
            val codeSourceLoc = UI4Bootstrap::class.java.protectionDomain?.codeSource?.location ?: return null
            val file = File(codeSourceLoc.toURI())
            if (file.isFile && file.name.endsWith(".jar")) file else null
        } catch (_: Exception) {
            null
        }
    }

    private fun extractUi4Classes(sourceJar: File, targetJar: File): Boolean {
        try {
            JarFile(sourceJar).use { jarIn ->
                val ui4Entries = jarIn.entries().asSequence()
                    .filter { (it.name.startsWith("ui4/") || it.name.endsWith(".kotlin_module")) && !it.isDirectory }
                    .toList()
                if (ui4Entries.isEmpty()) return false

                targetJar.parentFile?.mkdirs()
                val tempTarget = File(targetDir(targetJar), targetJar.name + ".tmp")
                JarOutputStream(FileOutputStream(tempTarget)).use { jarOut ->
                    val buffer = ByteArray(8192)
                    val addedDirs = mutableSetOf<String>()

                    for (entry in ui4Entries) {
                        val parts = entry.name.split("/")
                        var dirPath = ""
                        for (p in 0 until parts.size - 1) {
                            dirPath += parts[p] + "/"
                            if (addedDirs.add(dirPath)) {
                                val dirEntry = ZipEntry(dirPath)
                                dirEntry.time = entry.time
                                jarOut.putNextEntry(dirEntry)
                                jarOut.closeEntry()
                            }
                        }

                        val newEntry = ZipEntry(entry.name)
                        newEntry.time = entry.time
                        jarOut.putNextEntry(newEntry)
                        jarIn.getInputStream(entry).use { input ->
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                jarOut.write(buffer, 0, read)
                            }
                        }
                        jarOut.closeEntry()
                    }
                }
                if (targetJar.exists()) targetJar.delete()
                return tempTarget.renameTo(targetJar)
            }
        } catch (_: Exception) {
            return false
        }
    }

    private fun targetDir(targetJar: File): File = targetJar.parentFile ?: File(".")
}

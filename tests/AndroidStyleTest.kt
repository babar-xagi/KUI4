package tests

import kui.dex.ClassToDexCompiler
import kui.dex.UiMetadata
import java.io.File
import java.nio.file.Files

fun main() {
    val root = Files.createTempDirectory("kui-android-style-").toFile()
    try {
        val source = File(root, "src/main.kt").apply { parentFile.mkdirs() }
        source.writeText("""screen(backgroundColor = Color.hex("#121212")) { column(alignment = Alignment.TopEnd) { text("Kotlin only") } }""")
        val metadata = UiMetadata.fromProject(root, "Kotlin only")
        check(metadata.background == 0xFF121212.toInt())
        check(metadata.textColor == 0xFFF9FAFB.toInt())
        check(metadata.gravity == 53)
        val dex = ClassToDexCompiler.compileDirectory(File(root, "classes"), packageName = "com.kui.styles", projectRoot = root)
        check(dex.toString(Charsets.ISO_8859_1).contains("setBackgroundColor"))
        check(dex.toString(Charsets.ISO_8859_1).contains("setTextColor"))
        source.writeText("""screen(backgroundColor = Color.White) { text("Hello") }""")
        check(UiMetadata.fromProject(root, "Hello").textColor == 0xFF111827.toInt())
        source.writeText("""screen { text("white") }""")
        check(UiMetadata.fromProject(root, "white").background == null)
        println("Summary: 7 PASSED, 0 FAILED")
    } finally { root.deleteRecursively() }
}

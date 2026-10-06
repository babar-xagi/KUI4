package kui.dex

import java.io.File
import ui4.core.Color

/** Literal styling supported by the generated Android text Activity. */
data class UiMetadata(val text: String, val background: Int? = null, val textColor: Int? = null, val gravity: Int = 17) {
    companion object {
        fun fromProject(root: File?, text: String): UiMetadata {
            val source = root?.let { File(it, "src") }?.walkTopDown()
                ?.filter { it.isFile && it.extension == "kt" }?.sortedBy { it.path }
                ?.joinToString("\n") { it.readText() }.orEmpty()
            val colorValue = Regex("""(?:backgroundColor\s*=\s*|\.background(?:Color)?\s*\(\s*)(?:Color\.(?:hex|parse)\s*\(\s*"([^"]+)"\s*\)|Color\.([A-Za-z]+)|"([^"]+)")""")
                .find(source)?.groupValues?.drop(1)?.firstOrNull { it.isNotEmpty() }
            val background = colorValue?.let { runCatching { Color.parse(it).argb.toInt() }.getOrNull() }
            val textColor = background?.let {
                val brightness = 0.299 * ((it ushr 16) and 255) + 0.587 * ((it ushr 8) and 255) + 0.114 * (it and 255)
                if (brightness > 128) 0xFF111827.toInt() else 0xFFF9FAFB.toInt()
            }
            val alignment = Regex("""\balignment\s*=\s*Alignment\.([A-Za-z]+)""").find(source)?.groupValues?.get(1)
            val gravity = when (alignment) {
                "TopStart", "Start" -> 51
                "TopCenter", "CenterHorizontally" -> 49
                "TopEnd" -> 53
                "CenterStart" -> 19
                "CenterEnd" -> 21
                "BottomStart" -> 83
                "BottomCenter" -> 81
                "BottomEnd" -> 85
                else -> 17
            }
            return UiMetadata(text, background, textColor, gravity)
        }
    }
}

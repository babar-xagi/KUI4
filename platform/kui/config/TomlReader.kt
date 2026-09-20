package kui.config

import java.io.File

/**
 * Line token for diagnostic reporting.
 */
data class TomlEntry(
    val section: String,
    val key: String,
    val value: String,
    val lineNumber: Int
)

/**
 * Pure Kotlin zero-dependency TOML reader (v1).
 */
class TomlDocument(val entries: List<TomlEntry>) {
    fun getString(section: String, key: String): String? =
        entries.firstOrNull { it.section.equals(section, true) && it.key.equals(key, true) }?.value

    fun getInt(section: String, key: String): Int? =
        getString(section, key)?.toIntOrNull()

    fun getBoolean(section: String, key: String): Boolean? =
        getString(section, key)?.toBooleanStrictOrNull()

    fun findEntry(section: String, key: String): TomlEntry? =
        entries.firstOrNull { it.section.equals(section, true) && it.key.equals(key, true) }
}

object TomlReader {
    fun parse(content: String): TomlDocument {
        val entries = mutableListOf<TomlEntry>()
        var currentSection = ""

        content.lines().forEachIndexed { index, rawLine ->
            val lineNumber = index + 1
            // Remove comments
            val lineWithoutComment = rawLine.substringBefore('#').trim()
            if (lineWithoutComment.isEmpty()) return@forEachIndexed

            if (lineWithoutComment.startsWith("[") && lineWithoutComment.endsWith("]")) {
                currentSection = lineWithoutComment.substring(1, lineWithoutComment.length - 1).trim()
            } else if (lineWithoutComment.contains("=")) {
                val key = lineWithoutComment.substringBefore("=").trim()
                var rawValue = lineWithoutComment.substringAfter("=").trim()
                // Strip quotes if string
                if ((rawValue.startsWith("\"") && rawValue.endsWith("\"")) ||
                    (rawValue.startsWith("'") && rawValue.endsWith("'"))) {
                    rawValue = rawValue.substring(1, rawValue.length - 1)
                }
                entries.add(TomlEntry(currentSection, key, rawValue, lineNumber))
            }
        }

        return TomlDocument(entries)
    }

    fun parse(file: File): TomlDocument = parse(file.readText())
}

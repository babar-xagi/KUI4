package ui4.text

import ui4.core.Color

/**
 * Typographic font weight specification (Phase 126).
 */
enum class FontWeight(val value: Int) {
    Normal(400),
    Medium(500),
    Bold(700)
}

/**
 * Text alignment within its allocated layout width (Phase 128).
 */
enum class TextAlign {
    Start,
    Center,
    End,
    Justify
}

/**
 * Reading and layout directionality (Phase 130).
 */
enum class LayoutDirection {
    Ltr,
    Rtl
}

/**
 * Complete typography definition with styling and layout parameters (Phase 126).
 */
data class TextConfig(
    val fontSize: Float = 14f,
    val fontWeight: FontWeight = FontWeight.Normal,
    val color: Color? = null,
    val fontFamily: String = "System",
    val textAlign: TextAlign = TextAlign.Start,
    val layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    val lineHeightMultiplier: Float = 1.35f
) {
    val charWidth: Float get() = fontSize * (if (fontWeight == FontWeight.Bold) 0.62f else 0.58f)
    val lineHeight: Float get() = fontSize * lineHeightMultiplier
}

/**
 * Selection index range within text content (Phase 133).
 */
data class TextRange(val start: Int, val end: Int) {
    val isCollapsed: Boolean get() = start == end
    val min: Int get() = minOf(start, end)
    val max: Int get() = maxOf(start, end)
    val length: Int get() = max - min

    fun slice(text: String): String {
        val safeMin = min.coerceIn(0, text.length)
        val safeMax = max.coerceIn(0, text.length)
        return text.substring(safeMin, safeMax)
    }

    fun replace(text: String, replacement: String): String {
        val safeMin = min.coerceIn(0, text.length)
        val safeMax = max.coerceIn(0, text.length)
        return text.substring(0, safeMin) + replacement + text.substring(safeMax)
    }

    companion object {
        val Zero = TextRange(0, 0)
        fun collapsed(index: Int) = TextRange(index, index)
    }
}

/**
 * Text layout and line-wrapping algorithm (Phases 127, 128, 130).
 */
object TextLayout {

    data class WrappedLine(
        val text: String,
        val width: Float,
        val offsetX: Float
    )

    /**
     * Splits text into wrapped lines fitting within maxWidth (Phase 127).
     */
    fun wrapText(
        text: String,
        maxWidth: Float,
        config: TextConfig
    ): List<WrappedLine> {
        if (text.isEmpty()) {
            return listOf(WrappedLine("", 0f, 0f))
        }

        val charW = config.charWidth
        val maxCharsPerLine = maxOf(1, (maxWidth / charW).toInt())
        val lines = mutableListOf<String>()

        val rawParagraphs = text.split("\n")
        for (para in rawParagraphs) {
            if (para.isEmpty()) {
                lines.add("")
                continue
            }

            val words = para.split(" ")
            var currentLine = StringBuilder()

            for (word in words) {
                if (currentLine.isEmpty()) {
                    if (word.length > maxCharsPerLine) {
                        // Word exceeds full line; chunk it
                        var remaining = word
                        while (remaining.length > maxCharsPerLine) {
                            lines.add(remaining.substring(0, maxCharsPerLine))
                            remaining = remaining.substring(maxCharsPerLine)
                        }
                        currentLine.append(remaining)
                    } else {
                        currentLine.append(word)
                    }
                } else {
                    val candidate = currentLine.toString() + " " + word
                    if (candidate.length <= maxCharsPerLine) {
                        currentLine.append(" ").append(word)
                    } else {
                        lines.add(currentLine.toString())
                        currentLine = StringBuilder()
                        if (word.length > maxCharsPerLine) {
                            var remaining = word
                            while (remaining.length > maxCharsPerLine) {
                                lines.add(remaining.substring(0, maxCharsPerLine))
                                remaining = remaining.substring(maxCharsPerLine)
                            }
                            currentLine.append(remaining)
                        } else {
                            currentLine.append(word)
                        }
                    }
                }
            }

            if (currentLine.isNotEmpty()) {
                lines.add(currentLine.toString())
            }
        }

        // Calculate alignment offsets per line (Phase 128, 130)
        return lines.map { lineStr ->
            val lineWidth = lineStr.length * charW
            val effectiveAlign = when (config.layoutDirection) {
                LayoutDirection.Ltr -> config.textAlign
                LayoutDirection.Rtl -> when (config.textAlign) {
                    TextAlign.Start -> TextAlign.End
                    TextAlign.End -> TextAlign.Start
                    else -> config.textAlign
                }
            }

            val offsetX = when (effectiveAlign) {
                TextAlign.Start -> 0f
                TextAlign.Center -> maxOf(0f, (maxWidth - lineWidth) / 2f)
                TextAlign.End -> maxOf(0f, maxWidth - lineWidth)
                TextAlign.Justify -> 0f
            }

            WrappedLine(lineStr, lineWidth, offsetX)
        }
    }
}

/**
 * Multilingual and Unicode script verification utilities (Phases 131, 132).
 */
object UnicodeProof {

    /**
     * Detects if text contains Arabic or Urdu script characters (Phase 131).
     */
    fun isArabicOrUrdu(text: String): Boolean {
        for (char in text) {
            val code = char.code
            // Arabic, Arabic Supplement, Arabic Extended-A/B
            if (code in 0x0600..0x06FF || code in 0x0750..0x077F || code in 0x08A0..0x08FF) {
                return true
            }
        }
        return false
    }

    /**
     * Infers natural layout direction based on character content (Phase 130, 131).
     */
    fun inferDirection(text: String): LayoutDirection =
        if (isArabicOrUrdu(text)) LayoutDirection.Rtl else LayoutDirection.Ltr

    /**
     * Splits string into true grapheme clusters without breaking surrogate pairs or emojis (Phase 132).
     */
    fun extractGraphemes(text: String): List<String> {
        val clusters = mutableListOf<String>()
        var i = 0
        while (i < text.length) {
            val codePoint = text.codePointAt(i)
            val charCount = Character.charCount(codePoint)
            val nextI = i + charCount

            // Check for Zero-Width Joiner sequence (e.g. complex emojis)
            if (nextI < text.length && text[nextI] == '\u200D' && nextI + 1 < text.length) {
                val zwiCodePoint = text.codePointAt(nextI + 1)
                val zwiCount = Character.charCount(zwiCodePoint)
                val fullCluster = text.substring(i, nextI + 1 + zwiCount)
                clusters.add(fullCluster)
                i = nextI + 1 + zwiCount
            } else {
                clusters.add(text.substring(i, nextI))
                i = nextI
            }
        }
        return clusters
    }

    /**
     * Counts visual grapheme clusters instead of UTF-16 code units (Phase 132).
     */
    fun graphemeLength(text: String): Int =
        extractGraphemes(text).size
}

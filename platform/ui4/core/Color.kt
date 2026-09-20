package ui4.core

/**
 * 32-bit ARGB Color representation with hex parsing and conversions (Phase 059).
 */
data class Color(val argb: Long) {

    constructor(red: Float, green: Float, blue: Float, alpha: Float = 1.0f) : this(
        (((alpha.coerceIn(0f, 1f) * 255f).toInt() and 0xFF).toLong() shl 24) or
        (((red.coerceIn(0f, 1f) * 255f).toInt() and 0xFF).toLong() shl 16) or
        (((green.coerceIn(0f, 1f) * 255f).toInt() and 0xFF).toLong() shl 8) or
        (((blue.coerceIn(0f, 1f) * 255f).toInt() and 0xFF).toLong())
    )

    val alpha: Float get() = ((argb ushr 24) and 0xFF).toFloat() / 255f
    val red: Float get() = ((argb ushr 16) and 0xFF).toFloat() / 255f
    val green: Float get() = ((argb ushr 8) and 0xFF).toFloat() / 255f
    val blue: Float get() = (argb and 0xFF).toFloat() / 255f

    val alphaInt: Int get() = ((argb ushr 24) and 0xFF).toInt()
    val redInt: Int get() = ((argb ushr 16) and 0xFF).toInt()
    val greenInt: Int get() = ((argb ushr 8) and 0xFF).toInt()
    val blueInt: Int get() = (argb and 0xFF).toInt()

    fun copy(
        alpha: Float = this.alpha,
        red: Float = this.red,
        green: Float = this.green,
        blue: Float = this.blue
    ): Color = fromRgba(red, green, blue, alpha)

    /**
     * Formats color to #AARRGGBB hex representation.
     */
    fun toHexString(): String =
        "#%02X%02X%02X%02X".format(alphaInt, redInt, greenInt, blueInt)

    companion object {
        val Transparent = Color(0x00000000L)
        val Black = Color(0xFF000000L)
        val White = Color(0xFFFFFFFFL)
        val Red = Color(0xFFFF0000L)
        val Green = Color(0xFF00FF00L)
        val Blue = Color(0xFF0000FFL)
        val Yellow = Color(0xFFFFFF00L)
        val Cyan = Color(0xFF00FFFFL)
        val Magenta = Color(0xFFFF00FFL)
        val Gray = Color(0xFF888888L)
        val LightGray = Color(0xFFCCCCCCL)
        val DarkGray = Color(0xFF444444L)

        fun fromRgb(red: Int, green: Int, blue: Int): Color =
            fromArgb(255, red, green, blue)

        fun fromArgb(alpha: Int, red: Int, green: Int, blue: Int): Color {
            val a = (alpha and 0xFF).toLong() shl 24
            val r = (red and 0xFF).toLong() shl 16
            val g = (green and 0xFF).toLong() shl 8
            val b = (blue and 0xFF).toLong()
            return Color(a or r or g or b)
        }

        fun fromRgba(red: Float, green: Float, blue: Float, alpha: Float = 1.0f): Color {
            val a = (alpha.coerceIn(0f, 1f) * 255f).toInt()
            val r = (red.coerceIn(0f, 1f) * 255f).toInt()
            val g = (green.coerceIn(0f, 1f) * 255f).toInt()
            val b = (blue.coerceIn(0f, 1f) * 255f).toInt()
            return fromArgb(a, r, g, b)
        }

        /**
         * Parses hex strings: #RGB, #RGBA, #RRGGBB, #AARRGGBB.
         */
        fun parseHex(hex: String): Color {
            val clean = hex.removePrefix("#").trim()
            return when (clean.length) {
                3 -> { // RGB -> RRGGBB
                    val r = clean.substring(0, 1).repeat(2).toInt(16)
                    val g = clean.substring(1, 2).repeat(2).toInt(16)
                    val b = clean.substring(2, 3).repeat(2).toInt(16)
                    fromArgb(255, r, g, b)
                }
                4 -> { // RGBA -> RRGGBBAA
                    val r = clean.substring(0, 1).repeat(2).toInt(16)
                    val g = clean.substring(1, 2).repeat(2).toInt(16)
                    val b = clean.substring(2, 3).repeat(2).toInt(16)
                    val a = clean.substring(3, 4).repeat(2).toInt(16)
                    fromArgb(a, r, g, b)
                }
                6 -> { // RRGGBB -> AARRGGBB (A=FF)
                    val r = clean.substring(0, 2).toInt(16)
                    val g = clean.substring(2, 4).toInt(16)
                    val b = clean.substring(4, 6).toInt(16)
                    fromArgb(255, r, g, b)
                }
                8 -> { // AARRGGBB
                    val a = clean.substring(0, 2).toInt(16)
                    val r = clean.substring(2, 4).toInt(16)
                    val g = clean.substring(4, 6).toInt(16)
                    val b = clean.substring(6, 8).toInt(16)
                    fromArgb(a, r, g, b)
                }
                else -> throw IllegalArgumentException("Invalid hex color format: '$hex'")
            }
        }

        /**
         * Linear interpolation between two colors.
         */
        fun lerp(start: Color, end: Color, fraction: Float): Color {
            val f = fraction.coerceIn(0f, 1f)
            val a = start.alpha + (end.alpha - start.alpha) * f
            val r = start.red + (end.red - start.red) * f
            val g = start.green + (end.green - start.green) * f
            val b = start.blue + (end.blue - start.blue) * f
            return fromRgba(r, g, b, a)
        }
    }
}

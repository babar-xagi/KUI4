package ui4.core

/**
 * Layout box constraints governing minimum and maximum permissible dimensions (Phase 066).
 */
data class Constraints(
    val minWidth: Float = 0f,
    val maxWidth: Float = Float.POSITIVE_INFINITY,
    val minHeight: Float = 0f,
    val maxHeight: Float = Float.POSITIVE_INFINITY
) {
    init {
        require(minWidth >= 0f) { "minWidth cannot be negative: $minWidth" }
        require(minHeight >= 0f) { "minHeight cannot be negative: $minHeight" }
        require(maxWidth >= minWidth) { "maxWidth ($maxWidth) must be >= minWidth ($minWidth)" }
        require(maxHeight >= minHeight) { "maxHeight ($maxHeight) must be >= minHeight ($minHeight)" }
    }

    val hasBoundedWidth: Boolean get() = !maxWidth.isInfinite()
    val hasBoundedHeight: Boolean get() = !maxHeight.isInfinite()

    val isTightWidth: Boolean get() = minWidth >= maxWidth
    val isTightHeight: Boolean get() = minHeight >= maxHeight
    val isTight: Boolean get() = isTightWidth && isTightHeight

    val isZero: Boolean get() = maxWidth <= 0f || maxHeight <= 0f

    fun clampWidth(width: Float): Float =
        width.coerceIn(minWidth, maxWidth)

    fun clampHeight(height: Float): Float =
        height.coerceIn(minHeight, maxHeight)

    fun clamp(size: Size): Size =
        Size(clampWidth(size.width), clampHeight(size.height))

    fun deflate(insets: Insets): Constraints {
        val newMinW = maxOf(0f, minWidth - insets.horizontal)
        val newMaxW = if (maxWidth.isInfinite()) maxWidth else maxOf(newMinW, maxWidth - insets.horizontal)
        val newMinH = maxOf(0f, minHeight - insets.vertical)
        val newMaxH = if (maxHeight.isInfinite()) maxHeight else maxOf(newMinH, maxHeight - insets.vertical)
        return Constraints(newMinW, newMaxW, newMinH, newMaxH)
    }

    companion object {
        val Unconstrained = Constraints(0f, Float.POSITIVE_INFINITY, 0f, Float.POSITIVE_INFINITY)

        fun tight(size: Size): Constraints =
            Constraints(size.width, size.width, size.height, size.height)

        fun fixed(width: Float, height: Float): Constraints =
            Constraints(width, width, height, height)

        fun loose(size: Size): Constraints =
            Constraints(0f, size.width, 0f, size.height)
    }
}

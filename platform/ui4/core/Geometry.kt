package ui4.core

/**
 * 2D Point representation in density-independent pixels (Phase 058).
 */
data class Point(val x: Float = 0f, val y: Float = 0f) {
    operator fun plus(other: Point): Point = Point(x + other.x, y + other.y)
    operator fun minus(other: Point): Point = Point(x - other.x, y - other.y)

    companion object {
        val Zero = Point(0f, 0f)
    }
}

/**
 * 2D Size representation (Phase 058).
 */
data class Size(val width: Float = 0f, val height: Float = 0f) {
    val isEmpty: Boolean get() = width <= 0f || height <= 0f
    val area: Float get() = width * height

    companion object {
        val Zero = Size(0f, 0f)
        val Unspecified = Size(Float.NaN, Float.NaN)
    }
}

/**
 * 4-side Insets representing margins or padding (Phase 058, 070).
 */
data class Insets(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f
) {
    val horizontal: Float get() = left + right
    val vertical: Float get() = top + bottom

    companion object {
        val Zero = Insets(0f, 0f, 0f, 0f)

        fun all(value: Float): Insets = Insets(value, value, value, value)

        fun symmetric(horizontal: Float = 0f, vertical: Float = 0f): Insets =
            Insets(left = horizontal, top = vertical, right = horizontal, bottom = vertical)
    }
}

/**
 * 2D Rectangle bounds (Phase 058).
 */
data class Rect(
    val left: Float = 0f,
    val top: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f
) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height
    val centerX: Float get() = left + width / 2f
    val centerY: Float get() = top + height / 2f
    val size: Size get() = Size(width, height)
    val origin: Point get() = Point(left, top)

    fun contains(x: Float, y: Float): Boolean =
        x >= left && x <= right && y >= top && y <= bottom

    fun contains(p: Point): Boolean = contains(p.x, p.y)

    fun intersects(other: Rect): Boolean =
        left < other.right && right > other.left && top < other.bottom && bottom > other.top

    fun offset(dx: Float, dy: Float): Rect =
        Rect(left + dx, top + dy, width, height)

    fun deflate(insets: Insets): Rect =
        Rect(
            left = left + insets.left,
            top = top + insets.top,
            width = maxOf(0f, width - insets.horizontal),
            height = maxOf(0f, height - insets.vertical)
        )

    fun inflate(insets: Insets): Rect =
        Rect(
            left = left - insets.left,
            top = top - insets.top,
            width = width + insets.horizontal,
            height = height + insets.vertical
        )

    companion object {
        val Zero = Rect(0f, 0f, 0f, 0f)

        fun fromOriginAndSize(origin: Point, size: Size): Rect =
            Rect(origin.x, origin.y, size.width, size.height)
    }
}

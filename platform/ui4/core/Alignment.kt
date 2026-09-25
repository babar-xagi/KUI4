package ui4.core

/**
 * Horizontal alignment choices (Phase 071).
 */
enum class HorizontalAlign {
    Start,
    Center,
    End;

    fun align(childWidth: Float, availableWidth: Float): Float = when (this) {
        Start -> 0f
        Center -> (availableWidth - childWidth) / 2f
        End -> availableWidth - childWidth
    }

    companion object {
        val CenterHorizontally: HorizontalAlign = Center
    }
}

/**
 * Vertical alignment choices (Phase 071).
 */
enum class VerticalAlign {
    Top,
    Center,
    Bottom;

    fun align(childHeight: Float, availableHeight: Float): Float = when (this) {
        Top -> 0f
        Center -> (availableHeight - childHeight) / 2f
        Bottom -> availableHeight - childHeight
    }

    companion object {
        val CenterVertically: VerticalAlign = Center
    }
}

/**
 * 2D Alignment configuration (Phase 071).
 */
data class Alignment(
    val horizontal: HorizontalAlign = HorizontalAlign.Center,
    val vertical: VerticalAlign = VerticalAlign.Center
) {
    fun align(childSize: Size, availableSize: Size): Point {
        val x = horizontal.align(childSize.width, availableSize.width)
        val y = vertical.align(childSize.height, availableSize.height)
        return Point(x, y)
    }

    companion object {
        val TopStart = Alignment(HorizontalAlign.Start, VerticalAlign.Top)
        val TopCenter = Alignment(HorizontalAlign.Center, VerticalAlign.Top)
        val TopEnd = Alignment(HorizontalAlign.End, VerticalAlign.Top)

        val CenterStart = Alignment(HorizontalAlign.Start, VerticalAlign.Center)
        val Center = Alignment(HorizontalAlign.Center, VerticalAlign.Center)
        val CenterEnd = Alignment(HorizontalAlign.End, VerticalAlign.Center)

        val BottomStart = Alignment(HorizontalAlign.Start, VerticalAlign.Bottom)
        val BottomCenter = Alignment(HorizontalAlign.Center, VerticalAlign.Bottom)
        val BottomEnd = Alignment(HorizontalAlign.End, VerticalAlign.Bottom)

        // Directional and 1D Alignment Aliases (Jetpack Compose & KUI DSL compatibility)
        val CenterHorizontally = Alignment(HorizontalAlign.Center, VerticalAlign.Center)
        val CenterVertically = Alignment(HorizontalAlign.Center, VerticalAlign.Center)
        val Start = Alignment(HorizontalAlign.Start, VerticalAlign.Center)
        val End = Alignment(HorizontalAlign.End, VerticalAlign.Center)
        val Top = Alignment(HorizontalAlign.Center, VerticalAlign.Top)
        val Bottom = Alignment(HorizontalAlign.Center, VerticalAlign.Bottom)

        fun fromString(str: String): Alignment = when (str.lowercase()) {
            "start", "topstart", "top_start" -> TopStart
            "top", "topcenter", "top_center" -> TopCenter
            "topend", "top_end" -> TopEnd
            "centerstart", "center_start" -> CenterStart
            "center" -> Center
            "centerend", "center_end" -> CenterEnd
            "bottomstart", "bottom_start" -> BottomStart
            "bottom", "bottomcenter", "bottom_center" -> BottomCenter
            "bottomend", "bottom_end" -> BottomEnd
            else -> Center
        }
    }
}

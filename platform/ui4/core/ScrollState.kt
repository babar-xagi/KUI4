package ui4.core

/**
 * Observable scroll state with bounds clamping and direction indicators (Phases 120, 121).
 */
class ScrollState(initial: Float = 0f) {

    var scrollOffset: Float = initial
        private set

    var maxScrollOffset: Float = 0f
        internal set

    var viewportSize: Float = 0f
        internal set

    var contentSize: Float = 0f
        internal set

    val canScrollForward: Boolean
        get() = scrollOffset < maxScrollOffset

    val canScrollBackward: Boolean
        get() = scrollOffset > 0f

    /**
     * Updates the scroll position clamped to valid bounds [0, maxScrollOffset] (Phase 121).
     */
    fun scrollTo(target: Float): Float {
        val clamped = target.coerceIn(0f, maxOf(0f, maxScrollOffset))
        scrollOffset = clamped
        return scrollOffset
    }

    /**
     * Scrolls by a relative delta.
     */
    fun scrollBy(delta: Float): Float =
        scrollTo(scrollOffset + delta)

    /**
     * Updates viewport and content dimensions to recalculate maximum scroll boundary.
     */
    fun updateBounds(viewport: Float, content: Float) {
        viewportSize = viewport
        contentSize = content
        maxScrollOffset = maxOf(0f, content - viewport)
        scrollTo(scrollOffset)
    }

    override fun toString(): String =
        "ScrollState(offset=$scrollOffset, max=$maxScrollOffset, viewport=$viewportSize, content=$contentSize)"
}

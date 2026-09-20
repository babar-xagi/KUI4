package ui4.layout

import ui4.core.Constraints
import ui4.core.NodeId
import ui4.core.NodeIdGenerator
import ui4.core.Point
import ui4.core.Rect
import ui4.core.ScrollState
import ui4.core.Size
import ui4.core.clip
import ui4.tree.UiNode

/**
 * Scrollable vertical container node (Phases 120, 121).
 * Permits child content to expand beyond viewport height, clamping scroll offsets.
 */
class ScrollNode(
    val state: ScrollState = ScrollState(),
    id: NodeId = NodeIdGenerator.next()
) : UiNode(id, "Scroll") {

    init {
        // Scrolling containers clip their visual contents by default
        modifier = modifier.clip(true)
    }

    override fun measure(constraints: Constraints): Size {
        incomingConstraints = constraints
        val child = children.firstOrNull()

        if (child != null) {
            // Give child unbounded height
            val childConstraints = Constraints(
                minWidth = 0f,
                maxWidth = constraints.maxWidth,
                minHeight = 0f,
                maxHeight = Float.POSITIVE_INFINITY
            )
            child.measure(childConstraints)
        }

        // The ScrollNode itself takes the maximum available constrained height
        val desiredW = child?.measuredSize?.width ?: 0f
        val desiredH = if (constraints.hasBoundedHeight) constraints.maxHeight else (child?.measuredSize?.height ?: 0f)

        measuredSize = constraints.clamp(Size(desiredW, desiredH))
        return measuredSize
    }

    override fun layout(origin: Point, finalSize: Size) {
        bounds = Rect.fromOriginAndSize(origin, finalSize)
        val child = children.firstOrNull()
        if (child != null) {
            state.updateBounds(viewport = bounds.height, content = child.measuredSize.height)
            val childOrigin = Point(bounds.left, bounds.top - state.scrollOffset)
            child.layout(childOrigin, child.measuredSize)
        }
        dirty = false
    }

    override fun toString(): String = "Scroll$id offset=${state.scrollOffset} $bounds"
}

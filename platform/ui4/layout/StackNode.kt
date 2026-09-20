package ui4.layout

import ui4.core.Alignment
import ui4.core.Constraints
import ui4.core.Insets
import ui4.core.NodeId
import ui4.core.NodeIdGenerator
import ui4.core.Point
import ui4.core.Rect
import ui4.core.Size
import ui4.core.findFillHeight
import ui4.core.findFillWidth
import ui4.core.findPadding
import ui4.tree.UiNode

/**
 * Overlay stack layout positioning children on top of each other with Z-ordering (Phase 075).
 */
class StackNode(
    var alignment: Alignment = Alignment.TopStart,
    id: NodeId = NodeIdGenerator.next()
) : UiNode(id, "Stack") {

    override fun measure(constraints: Constraints): Size {
        incomingConstraints = constraints
        val padding = modifier.findPadding() ?: Insets.Zero
        val deflated = constraints.deflate(padding)
        val childConstraints = Constraints(
            minWidth = 0f,
            maxWidth = deflated.maxWidth,
            minHeight = 0f,
            maxHeight = deflated.maxHeight
        )

        var maxWidth = 0f
        var maxHeight = 0f

        for (child in children) {
            val childSize = child.measure(childConstraints)
            maxWidth = maxOf(maxWidth, childSize.width)
            maxHeight = maxOf(maxHeight, childSize.height)
        }

        var desiredW = maxWidth + padding.horizontal
        var desiredH = maxHeight + padding.vertical

        val fillW = modifier.findFillWidth()
        if (fillW != null && constraints.hasBoundedWidth) {
            desiredW = constraints.maxWidth * fillW
        }
        val fillH = modifier.findFillHeight()
        if (fillH != null && constraints.hasBoundedHeight) {
            desiredH = constraints.maxHeight * fillH
        }

        measuredSize = constraints.clamp(Size(desiredW, desiredH))
        return measuredSize
    }

    override fun layout(origin: Point, finalSize: Size) {
        bounds = Rect.fromOriginAndSize(origin, finalSize)
        val padding = modifier.findPadding() ?: Insets.Zero
        val innerArea = bounds.deflate(padding)

        // Position children in order (Z-index corresponds to list order)
        for (child in children) {
            val childSize = child.measuredSize
            val offset = alignment.align(childSize, innerArea.size)
            val childOrigin = Point(innerArea.left + offset.x, innerArea.top + offset.y)
            child.layout(childOrigin, childSize)
        }

        dirty = false
    }
}

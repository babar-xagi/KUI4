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
import ui4.core.findSize
import ui4.tree.UiNode

/**
 * Layout box primitive supporting alignment, padding, and size constraints (Phases 069, 070, 071).
 */
class BoxNode(
    var alignment: Alignment = Alignment.Center,
    id: NodeId = NodeIdGenerator.next()
) : UiNode(id, "Box") {

    private var contentBounds: Rect = Rect.Zero

    override fun measure(constraints: Constraints): Size {
        incomingConstraints = constraints
        val padding = modifier.findPadding() ?: Insets.Zero
        val deflatedConstraints = constraints.deflate(padding)
        val childConstraints = Constraints(
            minWidth = 0f,
            maxWidth = deflatedConstraints.maxWidth,
            minHeight = 0f,
            maxHeight = deflatedConstraints.maxHeight
        )

        var maxChildW = 0f
        var maxChildH = 0f

        for (child in children) {
            val childSize = child.measure(childConstraints)
            maxChildW = maxOf(maxChildW, childSize.width)
            maxChildH = maxOf(maxChildH, childSize.height)
        }

        var desiredW = maxChildW + padding.horizontal
        var desiredH = maxChildH + padding.vertical

        // Check explicit size modifier
        val sizeMod = modifier.findSize()
        if (sizeMod?.width != null) desiredW = sizeMod.width
        if (sizeMod?.height != null) desiredH = sizeMod.height

        // Check fill modifiers
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
        contentBounds = bounds.deflate(padding)

        for (child in children) {
            val childSize = child.measuredSize
            val offsetInBox = alignment.align(childSize, contentBounds.size)
            val childOrigin = Point(contentBounds.left + offsetInBox.x, contentBounds.top + offsetInBox.y)
            child.layout(childOrigin, childSize)
        }

        dirty = false
    }
}

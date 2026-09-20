package ui4.layout

import ui4.core.Constraints
import ui4.core.HorizontalAlign
import ui4.core.Insets
import ui4.core.NodeId
import ui4.core.NodeIdGenerator
import ui4.core.Point
import ui4.core.Rect
import ui4.core.Size
import ui4.core.findFillHeight
import ui4.core.findFillWidth
import ui4.core.findPadding
import ui4.core.findWeight
import ui4.tree.UiNode

/**
 * Vertical linear layout with spacing gap, horizontal alignment, and proportional flex weights
 * (Phases 072, 074, 076, 078).
 */
class ColumnNode(
    var gap: Float = 0f,
    var horizontalAlignment: HorizontalAlign = HorizontalAlign.Center,
    id: NodeId = NodeIdGenerator.next()
) : UiNode(id, "Column") {

    override fun measure(constraints: Constraints): Size {
        incomingConstraints = constraints
        val padding = modifier.findPadding() ?: Insets.Zero
        val deflated = constraints.deflate(padding)

        val unweightedChildren = mutableListOf<UiNode>()
        val weightedChildren = mutableListOf<Pair<UiNode, Float>>()
        var totalWeight = 0f

        for (child in children) {
            val weight = child.modifier.findWeight()
            if (weight != null && weight > 0f) {
                weightedChildren.add(child to weight)
                totalWeight += weight
            } else {
                unweightedChildren.add(child)
            }
        }

        var usedHeight = 0f
        var maxChildWidth = 0f
        val totalGaps = if (children.size > 1) (children.size - 1) * gap else 0f

        // 1. Measure unweighted children
        val unconstrainedChildConstraints = Constraints(
            minWidth = 0f,
            maxWidth = deflated.maxWidth,
            minHeight = 0f,
            maxHeight = Float.POSITIVE_INFINITY
        )

        for (child in unweightedChildren) {
            val size = child.measure(unconstrainedChildConstraints)
            usedHeight += size.height
            maxChildWidth = maxOf(maxChildWidth, size.width)
        }

        // 2. Measure weighted children
        val availableHeight = if (deflated.hasBoundedHeight) deflated.maxHeight else deflated.minHeight
        val remainingSpace = maxOf(0f, availableHeight - usedHeight - totalGaps)

        for ((child, weight) in weightedChildren) {
            val allocatedHeight = if (totalWeight > 0f) (remainingSpace * (weight / totalWeight)) else 0f
            val childConstraints = Constraints(
                minWidth = 0f,
                maxWidth = deflated.maxWidth,
                minHeight = allocatedHeight,
                maxHeight = allocatedHeight
            )
            val size = child.measure(childConstraints)
            usedHeight += size.height
            maxChildWidth = maxOf(maxChildWidth, size.width)
        }

        var desiredWidth = maxChildWidth + padding.horizontal
        var desiredHeight = usedHeight + totalGaps + padding.vertical

        // Check fill modifiers
        val fillW = modifier.findFillWidth()
        if (fillW != null && constraints.hasBoundedWidth) {
            desiredWidth = constraints.maxWidth * fillW
        }
        val fillH = modifier.findFillHeight()
        if (fillH != null && constraints.hasBoundedHeight) {
            desiredHeight = constraints.maxHeight * fillH
        }

        measuredSize = constraints.clamp(Size(desiredWidth, desiredHeight))
        return measuredSize
    }

    override fun layout(origin: Point, finalSize: Size) {
        bounds = Rect.fromOriginAndSize(origin, finalSize)
        val padding = modifier.findPadding() ?: Insets.Zero
        val innerArea = bounds.deflate(padding)

        var currentY = innerArea.top

        for (child in children) {
            val childSize = child.measuredSize
            val xOffset = horizontalAlignment.align(childSize.width, innerArea.width)
            val childOrigin = Point(innerArea.left + xOffset, currentY)

            child.layout(childOrigin, childSize)
            currentY += childSize.height + gap
        }

        dirty = false
    }
}

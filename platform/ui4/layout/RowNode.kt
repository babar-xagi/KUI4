package ui4.layout

import ui4.core.Constraints
import ui4.core.Insets
import ui4.core.NodeId
import ui4.core.NodeIdGenerator
import ui4.core.Point
import ui4.core.Rect
import ui4.core.Size
import ui4.core.VerticalAlign
import ui4.core.findFillHeight
import ui4.core.findFillWidth
import ui4.core.findPadding
import ui4.core.findSize
import ui4.core.findWeight
import ui4.tree.UiNode

/**
 * Horizontal linear layout with spacing gap, vertical alignment, and proportional flex weights
 * (Phases 073, 074, 077, 078).
 */
class RowNode(
    var gap: Float = 0f,
    var verticalAlignment: VerticalAlign = VerticalAlign.Center,
    id: NodeId = NodeIdGenerator.next()
) : UiNode(id, "Row") {

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

        var usedWidth = 0f
        var maxChildHeight = 0f
        val totalGaps = if (children.size > 1) (children.size - 1) * gap else 0f

        // 1. Measure unweighted children
        val unconstrainedChildConstraints = Constraints(
            minWidth = 0f,
            maxWidth = Float.POSITIVE_INFINITY,
            minHeight = 0f,
            maxHeight = deflated.maxHeight
        )

        for (child in unweightedChildren) {
            val size = child.measure(unconstrainedChildConstraints)
            usedWidth += size.width
            maxChildHeight = maxOf(maxChildHeight, size.height)
        }

        // 2. Measure weighted children
        val availableWidth = if (deflated.hasBoundedWidth) deflated.maxWidth else deflated.minWidth
        val remainingSpace = maxOf(0f, availableWidth - usedWidth - totalGaps)

        for ((child, weight) in weightedChildren) {
            val allocatedWidth = if (totalWeight > 0f) (remainingSpace * (weight / totalWeight)) else 0f
            val childConstraints = Constraints(
                minWidth = allocatedWidth,
                maxWidth = allocatedWidth,
                minHeight = 0f,
                maxHeight = deflated.maxHeight
            )
            val size = child.measure(childConstraints)
            usedWidth += size.width
            maxChildHeight = maxOf(maxChildHeight, size.height)
        }

        var desiredWidth = usedWidth + totalGaps + padding.horizontal
        var desiredHeight = maxChildHeight + padding.vertical

        // Check explicit size modifier
        val sizeMod = modifier.findSize()
        if (sizeMod?.width != null) desiredWidth = sizeMod.width
        if (sizeMod?.height != null) desiredHeight = sizeMod.height

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

        var currentX = innerArea.left

        for (child in children) {
            val childSize = child.measuredSize
            val yOffset = verticalAlignment.align(childSize.height, innerArea.height)
            val childOrigin = Point(currentX, innerArea.top + yOffset)

            child.layout(childOrigin, childSize)
            currentX += childSize.width + gap
        }

        dirty = false
    }
}

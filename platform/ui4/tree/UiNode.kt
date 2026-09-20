package ui4.tree

import ui4.core.Constraints
import ui4.core.Modifier
import ui4.core.NodeId
import ui4.core.NodeIdGenerator
import ui4.core.Point
import ui4.core.Rect
import ui4.core.Size
import ui4.core.findFillHeight
import ui4.core.findFillWidth
import ui4.core.findSize

/**
 * Base UI Node in the UI4 hierarchical tree (Phase 060).
 */
open class UiNode(
    val id: NodeId = NodeIdGenerator.next(),
    var tag: String = "Node"
) {
    var parent: UiNode? = null
        internal set

    val children: MutableList<UiNode> = mutableListOf()

    var bounds: Rect = Rect.Zero

    var dirty: Boolean = true
        internal set

    var modifier: Modifier = Modifier

    var measuredSize: Size = Size.Zero
        protected set

    var incomingConstraints: Constraints = Constraints.Unconstrained
        protected set

    /**
     * Measurement pass (Phase 067). Subclasses compute desired size within constraints.
     */
    open fun measure(constraints: Constraints): Size {
        incomingConstraints = constraints
        var w = 0f
        var h = 0f

        val sizeMod = modifier.findSize()
        if (sizeMod?.width != null) w = sizeMod.width
        if (sizeMod?.height != null) h = sizeMod.height

        val fillW = modifier.findFillWidth()
        if (fillW != null && constraints.hasBoundedWidth) {
            w = constraints.maxWidth * fillW
        }

        val fillH = modifier.findFillHeight()
        if (fillH != null && constraints.hasBoundedHeight) {
            h = constraints.maxHeight * fillH
        }

        measuredSize = constraints.clamp(Size(w, h))
        return measuredSize
    }

    /**
     * Layout pass (Phase 068). Positions this node and assigns child coordinates.
     */
    open fun layout(origin: Point, finalSize: Size) {
        bounds = Rect.fromOriginAndSize(origin, finalSize)
        dirty = false
    }

    override fun toString(): String = "$tag$id $bounds"
}

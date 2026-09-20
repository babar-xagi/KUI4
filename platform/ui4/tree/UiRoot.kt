package ui4.tree

import ui4.core.Constraints
import ui4.core.NodeId
import ui4.core.NodeIdGenerator
import ui4.core.Point
import ui4.core.Size

/**
 * Enforces single root node invariant for the UI component hierarchy (Phase 062).
 */
class UiRoot(
    id: NodeId = NodeIdGenerator.next()
) : UiNode(id, "Root") {

    var rootChild: UiNode?
        get() = children.firstOrNull()
        set(value) {
            clearChildren()
            if (value != null) {
                addChild(value)
            }
        }

    /**
     * Executes layout on the root and all its descendants with root viewport constraints.
     */
    fun performLayout(viewportWidth: Float, viewportHeight: Float) {
        val constraints = Constraints.fixed(viewportWidth, viewportHeight)
        measure(constraints)
        layout(Point.Zero, Size(viewportWidth, viewportHeight))
    }

    override fun measure(constraints: Constraints): Size {
        incomingConstraints = constraints
        val child = rootChild
        measuredSize = if (child != null) {
            child.measure(constraints)
        } else {
            Size.Zero
        }
        return measuredSize
    }

    override fun layout(origin: Point, finalSize: Size) {
        super.layout(origin, finalSize)
        rootChild?.layout(origin, finalSize)
    }
}

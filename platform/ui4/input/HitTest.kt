package ui4.input

import ui4.core.Point
import ui4.tree.UiNode

/**
 * Spatial hit-testing engine mapping pointer coordinates to target UI nodes (Phase 107).
 * Follows reverse Z-order traversal (topmost layered children tested first).
 */
object HitTest {

    /**
     * Finds the topmost leaf node containing the given point.
     */
    fun hitTest(root: UiNode, point: Point): UiNode? {
        if (!root.bounds.contains(point)) {
            return null
        }

        // Test children in reverse Z-order (topmost child first)
        for (i in root.children.indices.reversed()) {
            val child = root.children[i]
            val hit = hitTest(child, point)
            if (hit != null) {
                return hit
            }
        }

        // If no child captured the point, root itself was hit
        return root
    }

    /**
     * Resolves the complete ancestor chain from the hit leaf node up to root.
     */
    fun hitTestChain(root: UiNode, point: Point): List<UiNode> {
        val leaf = hitTest(root, point) ?: return emptyList()
        val chain = mutableListOf<UiNode>()
        var curr: UiNode? = leaf
        while (curr != null) {
            chain.add(curr)
            curr = curr.parent
        }
        return chain
    }
}

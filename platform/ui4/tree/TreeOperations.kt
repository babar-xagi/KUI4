package ui4.tree

/**
 * Robust tree mutation operations maintaining structural invariants (Phase 061).
 */

fun UiNode.addChild(child: UiNode) {
    insertChildAt(children.size, child)
}

fun UiNode.insertChildAt(index: Int, child: UiNode) {
    require(child !== this) { "Cannot add node to itself: $this" }
    require(!isAncestorOf(child)) { "Circular tree structure detected: $child is an ancestor of $this" }

    // Detach from previous parent if attached
    child.parent?.removeChild(child)

    child.parent = this
    children.add(index, child)

    markDirty()
    child.markDirty()
}

fun UiNode.removeChild(child: UiNode): Boolean {
    val removed = children.remove(child)
    if (removed) {
        child.parent = null
        markDirty()
        child.markDirty()
    }
    return removed
}

fun UiNode.clearChildren() {
    for (child in children) {
        child.parent = null
        child.markDirty()
    }
    children.clear()
    markDirty()
}

/**
 * Returns true if this node is an ancestor of the target node.
 */
fun UiNode.isAncestorOf(target: UiNode): Boolean {
    var curr: UiNode? = parent
    while (curr != null) {
        if (curr === target) return true
        curr = curr.parent
    }
    return false
}

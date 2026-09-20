package ui4.tree

import ui4.core.NodeId

/**
 * Depth-first search (DFS) and hierarchy traversal algorithms (Phase 063).
 */

fun UiNode.traversePreOrder(action: (UiNode) -> Unit) {
    action(this)
    for (child in children) {
        child.traversePreOrder(action)
    }
}

fun UiNode.traversePostOrder(action: (UiNode) -> Unit) {
    for (child in children) {
        child.traversePostOrder(action)
    }
    action(this)
}

fun UiNode.findNodeById(targetId: NodeId): UiNode? {
    if (this.id == targetId) return this
    for (child in children) {
        val found = child.findNodeById(targetId)
        if (found != null) return found
    }
    return null
}

fun UiNode.collectAll(): List<UiNode> {
    val list = mutableListOf<UiNode>()
    traversePreOrder { list.add(it) }
    return list
}

fun UiNode.treeDepth(): Int {
    if (children.isEmpty()) return 1
    return 1 + (children.maxOfOrNull { it.treeDepth() } ?: 0)
}

fun UiNode.countNodes(): Int {
    var count = 0
    traversePreOrder { count++ }
    return count
}

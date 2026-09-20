package ui4.tree

/**
 * Fine-grained dirty flag tracking and subtree invalidation protocol (Phase 064).
 */

fun UiNode.markDirty() {
    if (!dirty) {
        dirty = true
        // Bubble up notification so ancestor layouts know to re-measure
        parent?.markDirty()
    }
}

fun UiNode.cleanDirty() {
    dirty = false
    for (child in children) {
        child.cleanDirty()
    }
}

fun UiNode.hasDirtyDescendant(): Boolean {
    if (dirty) return true
    for (child in children) {
        if (child.hasDirtyDescendant()) return true
    }
    return false
}

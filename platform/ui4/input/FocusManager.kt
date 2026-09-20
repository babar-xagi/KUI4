package ui4.input

import ui4.tree.UiNode
import ui4.tree.markDirty
import ui4.tree.traversePreOrder

/**
 * Manages focus traversal and current active focus target (Phase 112).
 */
class FocusManager(val root: UiNode) {

    var focusedNode: UiNode? = null
        private set

    /**
     * Requests focus for a specific node.
     * @return true if focus was successfully granted.
     */
    fun requestFocus(node: UiNode): Boolean {
        if (!node.isFocusable || !node.enabled) {
            return false
        }
        if (focusedNode === node) {
            return true
        }

        focusedNode?.let {
            it.isFocused = false
            it.markDirty()
        }

        node.isFocused = true
        node.markDirty()
        focusedNode = node
        return true
    }

    /**
     * Clears any active focus.
     */
    fun clearFocus() {
        focusedNode?.let {
            it.isFocused = false
            it.markDirty()
        }
        focusedNode = null
    }

    /**
     * Moves focus to the next focusable node in tree traversal order (wraps around).
     */
    fun focusNext(): Boolean {
        val candidates = collectFocusableNodes()
        if (candidates.isEmpty()) {
            clearFocus()
            return false
        }

        val currentIndex = focusedNode?.let { candidates.indexOf(it) } ?: -1
        val nextIndex = if (currentIndex == -1 || currentIndex == candidates.lastIndex) {
            0
        } else {
            currentIndex + 1
        }

        return requestFocus(candidates[nextIndex])
    }

    /**
     * Moves focus to the previous focusable node in tree traversal order (wraps around).
     */
    fun focusPrevious(): Boolean {
        val candidates = collectFocusableNodes()
        if (candidates.isEmpty()) {
            clearFocus()
            return false
        }

        val currentIndex = focusedNode?.let { candidates.indexOf(it) } ?: -1
        val prevIndex = if (currentIndex <= 0) {
            candidates.lastIndex
        } else {
            currentIndex - 1
        }

        return requestFocus(candidates[prevIndex])
    }

    fun collectFocusableNodes(): List<UiNode> {
        val focusables = mutableListOf<UiNode>()
        root.traversePreOrder { node ->
            if (node.isFocusable && node.enabled) {
                focusables.add(node)
            }
        }
        return focusables
    }
}

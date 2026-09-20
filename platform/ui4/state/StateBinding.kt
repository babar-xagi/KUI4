package ui4.state

import ui4.layout.TextNode
import ui4.tree.UiNode
import ui4.tree.markDirty

/**
 * Binds a reactive State to a UiNode for partial subtree updates (Phases 104, 105).
 */
fun <T, N : UiNode> N.bindState(
    state: State<T>,
    immediate: Boolean = true,
    onUpdate: (N, T) -> Unit
): Subscription {
    if (immediate) {
        onUpdate(this, state.value)
    }
    return state.subscribe { newValue ->
        onUpdate(this, newValue)
        // Mark only this node and its direct ancestor path dirty (Phase 105)
        this.markDirty()
    }
}

/**
 * Binds text content of a TextNode to an observable state (Phases 105, 106).
 */
fun TextNode.bindText(state: State<*>): Subscription {
    return bindState(state) { node, value ->
        node.text = value.toString()
    }
}

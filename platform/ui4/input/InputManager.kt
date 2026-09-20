package ui4.input

import ui4.layout.ButtonNode
import ui4.tree.UiNode
import ui4.tree.markDirty

/**
 * Dispatches input pointer and key events across the UI hierarchy (Phases 108–114).
 */
class InputManager(
    val root: UiNode,
    val focusManager: FocusManager = FocusManager(root)
) {
    var currentPressedNode: UiNode? = null
        private set

    /**
     * Dispatches a pointer event (Down, Move, Up, Cancel) to the UI tree (Phases 107–111).
     * @return true if the event was consumed.
     */
    fun dispatchPointer(event: PointerEvent): Boolean {
        return when (event.type) {
            PointerEventType.Down -> handlePointerDown(event)
            PointerEventType.Move -> handlePointerMove(event)
            PointerEventType.Up -> handlePointerUp(event)
            PointerEventType.Cancel -> handlePointerCancel(event)
        }
    }

    private fun handlePointerDown(event: PointerEvent): Boolean {
        val target = HitTest.hitTest(root, event.position) ?: return false

        // Disabled nodes ignore pointer events (Phase 111)
        if (!target.enabled) {
            return false
        }

        // If target is focusable, give it focus
        if (target.isFocusable) {
            focusManager.requestFocus(target)
        }

        target.isPressed = true
        target.markDirty()
        currentPressedNode = target
        return true
    }

    private fun handlePointerMove(event: PointerEvent): Boolean {
        val pressed = currentPressedNode ?: return false
        val inside = pressed.bounds.contains(event.position)
        if (pressed.isPressed != inside) {
            pressed.isPressed = inside
            pressed.markDirty()
        }
        return true
    }

    private fun handlePointerUp(event: PointerEvent): Boolean {
        val pressed = currentPressedNode ?: return false
        val inside = pressed.bounds.contains(event.position)

        // Click detection (Phase 109): pointer released inside the initially pressed node
        if (inside && pressed.enabled) {
            triggerClick(pressed)
        }

        pressed.isPressed = false
        pressed.markDirty()
        currentPressedNode = null
        return true
    }

    private fun handlePointerCancel(event: PointerEvent): Boolean {
        currentPressedNode?.let {
            it.isPressed = false
            it.markDirty()
        }
        currentPressedNode = null
        return true
    }

    /**
     * Dispatches a keyboard event to the active focus target (Phases 113, 114).
     * @return true if consumed.
     */
    fun dispatchKey(event: KeyEvent): Boolean {
        // Tab key navigation (Phase 112)
        if (event.code == KeyCode.Tab && event.isActionDown) {
            return if (event.isShiftPressed) {
                focusManager.focusPrevious()
            } else {
                focusManager.focusNext()
            }
        }

        // Keyboard activation via Enter or Space (Phase 114)
        if (event.code == KeyCode.Enter || event.code == KeyCode.Space) {
            val focused = focusManager.focusedNode
            if (focused != null && focused.enabled) {
                if (event.isActionDown) {
                    focused.isPressed = true
                    focused.markDirty()
                } else if (event.isActionUp) {
                    focused.isPressed = false
                    focused.markDirty()
                    triggerClick(focused)
                }
                return true
            }
        }

        return false
    }

    /**
     * Invokes the click callback on a target node (Phases 109, 110).
     */
    private fun triggerClick(node: UiNode) {
        if (!node.enabled) return

        if (node is ButtonNode) {
            node.click()
        } else {
            node.onClick?.invoke()
        }
    }
}

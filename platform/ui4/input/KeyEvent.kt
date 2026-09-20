package ui4.input

/**
 * Standard key identifiers supported by UI4 input handling (Phase 113).
 */
enum class KeyCode {
    Enter,
    Space,
    Tab,
    Escape,
    ArrowUp,
    ArrowDown,
    ArrowLeft,
    ArrowRight,
    Backspace,
    Delete,
    Other
}

/**
 * Key actuation action.
 */
enum class KeyAction {
    Down,
    Up
}

/**
 * Platform-independent keyboard event model (Phase 113).
 */
data class KeyEvent(
    val code: KeyCode,
    val action: KeyAction = KeyAction.Down,
    val isShiftPressed: Boolean = false,
    val isCtrlPressed: Boolean = false,
    val isAltPressed: Boolean = false
) {
    val isActionDown: Boolean get() = action == KeyAction.Down
    val isActionUp: Boolean get() = action == KeyAction.Up
}

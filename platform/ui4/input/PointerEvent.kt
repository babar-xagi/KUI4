package ui4.input

import ui4.core.Point

/**
 * Types of pointer motion / contact events (Phases 107, 108, 115).
 */
enum class PointerEventType {
    Down,
    Move,
    Up,
    Cancel
}

/**
 * Input device pointer classification.
 */
enum class PointerType {
    Touch,
    Mouse,
    Stylus
}

/**
 * Platform-independent pointer event data (Phases 107, 108, 115).
 */
data class PointerEvent(
    val type: PointerEventType,
    val position: Point,
    val pointerId: Int = 0,
    val pointerType: PointerType = PointerType.Touch,
    val timestampNanos: Long = System.nanoTime()
) {
    val x: Float get() = position.x
    val y: Float get() = position.y
}

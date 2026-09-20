package ui4.gesture

import ui4.core.Point
import ui4.input.PointerEvent
import ui4.input.PointerEventType
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Recognizes long-press interactions held beyond a time threshold (Phase 117).
 */
class LongPressGestureRecognizer(
    val minDurationMs: Long = 500L,
    val slopDistance: Float = 10f,
    var onLongPress: ((Point) -> Unit)? = null
) : GestureRecognizer {

    override var state: GestureState = GestureState.Possible
        private set

    private var downTimeNanos: Long = 0L
    private var downPosition: Point? = null
    var hasFired: Boolean = false
        private set

    override fun processPointerEvent(event: PointerEvent): GestureState {
        when (event.type) {
            PointerEventType.Down -> {
                downTimeNanos = event.timestampNanos
                downPosition = event.position
                state = GestureState.Began
                hasFired = false
            }
            PointerEventType.Move -> {
                val start = downPosition
                if (start != null) {
                    val dx = event.position.x - start.x
                    val dy = event.position.y - start.y
                    if (sqrt(dx * dx + dy * dy) > slopDistance) {
                        state = GestureState.Failed
                    } else if (!hasFired) {
                        val elapsedMs = (event.timestampNanos - downTimeNanos) / 1_000_000L
                        if (elapsedMs >= minDurationMs) {
                            state = GestureState.Recognized
                            hasFired = true
                            onLongPress?.invoke(event.position)
                        } else {
                            state = GestureState.Changed
                        }
                    }
                }
            }
            PointerEventType.Up -> {
                if (state == GestureState.Began || state == GestureState.Changed) {
                    val elapsedMs = (event.timestampNanos - downTimeNanos) / 1_000_000L
                    if (elapsedMs >= minDurationMs && !hasFired) {
                        state = GestureState.Recognized
                        hasFired = true
                        onLongPress?.invoke(event.position)
                    } else {
                        state = GestureState.Failed
                    }
                } else if (!hasFired) {
                    state = GestureState.Failed
                }
            }
            PointerEventType.Cancel -> {
                state = GestureState.Cancelled
            }
        }
        return state
    }

    /**
     * Manually triggers time evaluation for clock-driven recognition.
     */
    fun checkTimeout(currentTimeNanos: Long = System.nanoTime()): Boolean {
        if ((state == GestureState.Began || state == GestureState.Changed) && !hasFired) {
            val elapsedMs = (currentTimeNanos - downTimeNanos) / 1_000_000L
            if (elapsedMs >= minDurationMs) {
                state = GestureState.Recognized
                hasFired = true
                downPosition?.let { onLongPress?.invoke(it) }
                return true
            }
        }
        return false
    }

    override fun reset() {
        state = GestureState.Possible
        downPosition = null
        hasFired = false
    }
}

/**
 * Recognizes continuous drag gestures reporting positional deltas (Phase 118).
 */
class DragGestureRecognizer(
    val slopDistance: Float = 10f,
    var onDragStart: ((Point) -> Unit)? = null,
    var onDrag: ((Point, Point) -> Unit)? = null, // (currentPoint, delta)
    var onDragEnd: ((Point) -> Unit)? = null
) : GestureRecognizer {

    override var state: GestureState = GestureState.Possible
        private set

    private var startPosition: Point? = null
    private var lastPosition: Point? = null

    override fun processPointerEvent(event: PointerEvent): GestureState {
        when (event.type) {
            PointerEventType.Down -> {
                startPosition = event.position
                lastPosition = event.position
                state = GestureState.Began
            }
            PointerEventType.Move -> {
                val start = startPosition ?: return state
                val last = lastPosition ?: start

                val totalDx = event.position.x - start.x
                val totalDy = event.position.y - start.y
                val dist = sqrt(totalDx * totalDx + totalDy * totalDy)

                if (state == GestureState.Began) {
                    if (dist >= slopDistance) {
                        state = GestureState.Recognized
                        onDragStart?.invoke(start)
                        val delta = Point(event.position.x - last.x, event.position.y - last.y)
                        onDrag?.invoke(event.position, delta)
                        lastPosition = event.position
                    }
                } else if (state == GestureState.Recognized || state == GestureState.Changed) {
                    state = GestureState.Changed
                    val delta = Point(event.position.x - last.x, event.position.y - last.y)
                    onDrag?.invoke(event.position, delta)
                    lastPosition = event.position
                }
            }
            PointerEventType.Up -> {
                if (state == GestureState.Recognized || state == GestureState.Changed) {
                    onDragEnd?.invoke(event.position)
                } else {
                    state = GestureState.Failed
                }
            }
            PointerEventType.Cancel -> {
                state = GestureState.Cancelled
            }
        }
        return state
    }

    override fun reset() {
        state = GestureState.Possible
        startPosition = null
        lastPosition = null
    }
}

/**
 * Direction classifications for swipe gestures (Phase 119).
 */
enum class SwipeDirection {
    Left,
    Right,
    Up,
    Down
}

/**
 * Recognizes fast directional swipe gestures (Phase 119).
 */
class SwipeGestureRecognizer(
    val minDistance: Float = 30f,
    val maxDurationMs: Long = 400L,
    var onSwipe: ((SwipeDirection) -> Unit)? = null
) : GestureRecognizer {

    override var state: GestureState = GestureState.Possible
        private set

    private var startPos: Point? = null
    private var startTimeNanos: Long = 0L

    override fun processPointerEvent(event: PointerEvent): GestureState {
        when (event.type) {
            PointerEventType.Down -> {
                startPos = event.position
                startTimeNanos = event.timestampNanos
                state = GestureState.Began
            }
            PointerEventType.Move -> {
                if (state == GestureState.Began) {
                    state = GestureState.Changed
                }
            }
            PointerEventType.Up -> {
                val start = startPos
                if (start != null) {
                    val elapsedMs = (event.timestampNanos - startTimeNanos) / 1_000_000L
                    val dx = event.position.x - start.x
                    val dy = event.position.y - start.y
                    val absDx = abs(dx)
                    val absDy = abs(dy)

                    if (elapsedMs <= maxDurationMs && (absDx >= minDistance || absDy >= minDistance)) {
                        val dir = if (absDx > absDy) {
                            if (dx > 0) SwipeDirection.Right else SwipeDirection.Left
                        } else {
                            if (dy > 0) SwipeDirection.Down else SwipeDirection.Up
                        }
                        state = GestureState.Recognized
                        onSwipe?.invoke(dir)
                    } else {
                        state = GestureState.Failed
                    }
                } else {
                    state = GestureState.Failed
                }
            }
            PointerEventType.Cancel -> {
                state = GestureState.Cancelled
            }
        }
        return state
    }

    override fun reset() {
        state = GestureState.Possible
        startPos = null
    }
}

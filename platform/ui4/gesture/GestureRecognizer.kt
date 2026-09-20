package ui4.gesture

import ui4.core.Point
import ui4.input.PointerEvent
import ui4.input.PointerEventType
import kotlin.math.sqrt

/**
 * Lifecycle states of an active gesture recognition process (Phase 115).
 */
enum class GestureState {
    Possible,
    Began,
    Changed,
    Recognized,
    Failed,
    Cancelled
}

/**
 * Universal gesture recognizer contract (Phase 115).
 * Processes a stream of pointer events and transitions through recognition states.
 */
interface GestureRecognizer {
    val state: GestureState
    fun processPointerEvent(event: PointerEvent): GestureState
    fun reset()
}

/**
 * Standard single-pointer tap recognizer with displacement threshold (Phase 115, 116).
 */
class TapGestureRecognizer(
    val slopDistance: Float = 10f,
    var onTap: ((Point) -> Unit)? = null
) : GestureRecognizer {

    override var state: GestureState = GestureState.Possible
        private set

    private var startPosition: Point? = null

    override fun processPointerEvent(event: PointerEvent): GestureState {
        when (event.type) {
            PointerEventType.Down -> {
                startPosition = event.position
                state = GestureState.Began
            }
            PointerEventType.Move -> {
                val start = startPosition
                if (start != null) {
                    val dx = event.position.x - start.x
                    val dy = event.position.y - start.y
                    val dist = sqrt(dx * dx + dy * dy)
                    if (dist > slopDistance) {
                        state = GestureState.Failed
                    } else {
                        state = GestureState.Changed
                    }
                }
            }
            PointerEventType.Up -> {
                if (state == GestureState.Began || state == GestureState.Changed) {
                    val finalPoint = event.position
                    state = GestureState.Recognized
                    onTap?.invoke(finalPoint)
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
    }
}

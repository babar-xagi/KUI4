package ui4.state

/**
 * Subscription handle for cancelling an active observer (Phase 104).
 */
fun interface Subscription {
    fun unsubscribe()
}

/**
 * Read-only reactive state contract (Phases 101, 104).
 */
interface State<T> {
    val value: T
    fun subscribe(observer: (T) -> Unit): Subscription
}

/**
 * Mutable observable state container with change detection (Phases 101, 102, 103).
 */
interface MutableState<T> : State<T> {
    override var value: T
}

/**
 * Default implementation of observable state with equality check (Phases 101–104).
 */
class StateImpl<T>(initialValue: T) : MutableState<T> {

    private var _value: T = initialValue
    private val observers: MutableList<(T) -> Unit> = mutableListOf()

    override var value: T
        get() = _value
        set(newValue) {
            // Change detection (Phase 103): Skip notifying if value is structurally equal
            if (newValue == _value) return

            _value = newValue
            val snapshot = observers.toList()
            for (obs in snapshot) {
                obs(newValue)
            }
        }

    override fun subscribe(observer: (T) -> Unit): Subscription {
        observers.add(observer)
        return Subscription {
            observers.remove(observer)
        }
    }

    val subscriberCount: Int
        get() = observers.size

    override fun toString(): String = "State(value=$_value)"
}

/**
 * Creates a reactive mutable state container (Phase 101).
 */
fun <T> mutableStateOf(initial: T): MutableState<T> = StateImpl(initial)

/**
 * Convenience DSL function for creating a state container (Phase 101).
 */
fun <T> state(initial: T): MutableState<T> = StateImpl(initial)

/**
 * Delegate operator for reading reactive state: `val x by state`
 */
operator fun <T> State<T>.getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): T = value

/**
 * Delegate operator for mutating reactive state: `var x by mutableStateOf`
 */
operator fun <T> MutableState<T>.setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: T) {
    this.value = value
}

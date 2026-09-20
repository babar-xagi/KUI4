package ui4.navigation

/**
 * Screen destination identifier in a navigation graph (Phase 147).
 */
interface ScreenDestination {
    val name: String
}

/**
 * Standard named screen destination implementation.
 */
data class NamedScreen(override val name: String) : ScreenDestination

/**
 * Stack-based screen navigation controller (Phase 147).
 */
class Navigator(initialScreen: ScreenDestination) {

    private val _backStack: MutableList<ScreenDestination> = mutableListOf(initialScreen)
    private val listeners: MutableList<(ScreenDestination) -> Unit> = mutableListOf()

    val backStack: List<ScreenDestination>
        get() = _backStack.toList()

    val currentScreen: ScreenDestination
        get() = _backStack.last()

    val stackDepth: Int
        get() = _backStack.size

    val canPop: Boolean
        get() = _backStack.size > 1

    /**
     * Pushes a new screen onto the back stack (Phase 147).
     */
    fun push(screen: ScreenDestination) {
        _backStack.add(screen)
        notifyListeners()
    }

    /**
     * Pops the current screen returning to the previous screen (Phase 147).
     * @return true if popped; false if only one screen remains.
     */
    fun pop(): Boolean {
        if (!canPop) return false
        _backStack.removeAt(_backStack.lastIndex)
        notifyListeners()
        return true
    }

    /**
     * Replaces the current top screen with a new screen.
     */
    fun replace(screen: ScreenDestination) {
        if (_backStack.isNotEmpty()) {
            _backStack.removeAt(_backStack.lastIndex)
        }
        _backStack.add(screen)
        notifyListeners()
    }

    /**
     * Subscribes to navigation destination transitions.
     */
    fun addListener(listener: (ScreenDestination) -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: (ScreenDestination) -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        val dest = currentScreen
        for (listener in listeners.toList()) {
            listener(dest)
        }
    }
}

/**
 * Intercepts platform / Android back button presses to pop the navigation stack (Phase 148).
 */
class BackHandler(val navigator: Navigator) {

    /**
     * Handles back press.
     * @return true if consumed (screen popped); false if at root screen.
     */
    fun handleBackPressed(): Boolean =
        navigator.pop()
}

/**
 * Preserves component state across navigation screen transitions and recreations (Phase 149).
 */
class SavedStateRegistry {

    private val stateBundle: MutableMap<String, Any> = mutableMapOf()

    /**
     * Stores a state value under a designated key.
     */
    fun save(key: String, value: Any) {
        stateBundle[key] = value
    }

    /**
     * Restores a previously saved state value.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> restore(key: String): T? =
        stateBundle[key] as? T

    fun contains(key: String): Boolean =
        stateBundle.containsKey(key)

    fun remove(key: String): Any? =
        stateBundle.remove(key)

    fun clear() {
        stateBundle.clear()
    }

    val entryCount: Int
        get() = stateBundle.size
}

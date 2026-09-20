package ui4

/**
 * UI4 Declarative API Core Primitives (Phase 045).
 */

class UI4AppScope {
    fun screen(content: UI4ScreenScope.() -> Unit) {
        val scope = UI4ScreenScope()
        scope.content()
    }
}

class UI4ScreenScope {
    fun center(content: UI4ContainerScope.() -> Unit) {
        val scope = UI4ContainerScope()
        scope.content()
    }

    fun column(
        gap: Int = 0,
        alignment: String = "center",
        content: UI4ContainerScope.() -> Unit
    ) {
        val scope = UI4ContainerScope()
        scope.content()
    }

    fun row(
        gap: Int = 0,
        alignment: String = "center",
        content: UI4ContainerScope.() -> Unit
    ) {
        val scope = UI4ContainerScope()
        scope.content()
    }

    fun box(
        alignment: String = "center",
        content: UI4ContainerScope.() -> Unit
    ) {
        val scope = UI4ContainerScope()
        scope.content()
    }

    fun text(value: String, style: TextStyle = TextStyle.Body) {
        // UI node registration in later phases
    }

    fun button(label: String, onClick: () -> Unit = {}) {
        // UI node registration in later phases
    }
}

class UI4ContainerScope {
    fun text(value: String, style: TextStyle = TextStyle.Body) {}
    fun button(label: String, onClick: () -> Unit = {}) {}
    fun column(gap: Int = 0, content: UI4ContainerScope.() -> Unit) { content() }
    fun row(gap: Int = 0, content: UI4ContainerScope.() -> Unit) { content() }
    fun box(content: UI4ContainerScope.() -> Unit) { content() }
}

enum class TextStyle {
    Headline,
    Title,
    Body,
    Caption
}

class State<T>(var value: T)

fun <T> state(initial: T): State<T> = State(initial)

fun app(block: UI4AppScope.() -> Unit) {
    val appScope = UI4AppScope()
    appScope.block()
}

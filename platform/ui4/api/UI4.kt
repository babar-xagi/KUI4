package ui4

import ui4.core.*
import ui4.layout.BoxNode
import ui4.layout.ButtonNode
import ui4.layout.ColumnNode
import ui4.layout.RowNode
import ui4.layout.StackNode
import ui4.layout.TextNode
import ui4.layout.TextStyle
import ui4.tree.UiNode
import ui4.tree.UiRoot
import ui4.tree.addChild
import ui4.state.*
import ui4.text.FontWeight
import ui4.text.TextAlign

// Re-export core types to ui4 package for ergonomic imports (Phase 090)
typealias Color = ui4.core.Color
typealias Modifier = ui4.core.Modifier
typealias Alignment = ui4.core.Alignment
typealias HorizontalAlign = ui4.core.HorizontalAlign
typealias VerticalAlign = ui4.core.VerticalAlign
typealias TextStyle = ui4.layout.TextStyle
typealias Insets = ui4.core.Insets
typealias Point = ui4.core.Point
typealias Size = ui4.core.Size
typealias Rect = ui4.core.Rect
typealias Constraints = ui4.core.Constraints
typealias State<T> = ui4.state.State<T>
typealias MutableState<T> = ui4.state.MutableState<T>
typealias ScrollState = ui4.core.ScrollState
typealias ScrollNode = ui4.layout.ScrollNode
typealias TextFieldNode = ui4.layout.TextFieldNode
typealias Navigator = ui4.navigation.Navigator
typealias ScreenDestination = ui4.navigation.ScreenDestination
typealias NamedScreen = ui4.navigation.NamedScreen
typealias BackHandler = ui4.navigation.BackHandler
typealias SavedStateRegistry = ui4.navigation.SavedStateRegistry
typealias TextRange = ui4.text.TextRange
typealias TextAlign = ui4.text.TextAlign
typealias FontWeight = ui4.text.FontWeight
typealias LayoutDirection = ui4.text.LayoutDirection
typealias Role = ui4.accessibility.Role
typealias SemanticsNode = ui4.accessibility.SemanticsNode

fun <T> mutableStateOf(initial: T): MutableState<T> = ui4.state.mutableStateOf(initial)
fun <T> state(initial: T): MutableState<T> = ui4.state.state(initial)

operator fun <T> State<T>.getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): T = value
operator fun <T> MutableState<T>.setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: T) {
    this.value = value
}

/**
 * Top-level application scope for building UI trees (Phases 080, 081).
 */
class UI4AppScope {
    val root: UiRoot = UiRoot()

    fun screen(
        alignment: Alignment = Alignment.Center,
        modifier: Modifier = Modifier.fillMaxSize(),
        padding: Float = 0f,
        backgroundColor: Color? = null,
        background: Color? = null,
        color: Color? = null,
        content: UI4ContainerScope.() -> Unit
    ): UiRoot {
        var mod = modifier
        val bg = backgroundColor ?: background ?: color
        if (padding > 0f) mod = mod.padding(padding)
        if (bg != null) mod = mod.background(bg)
        val screenBox = BoxNode(alignment = alignment).apply {
            this.modifier = mod
            this.tag = "Screen"
        }
        root.rootChild = screenBox

        val scope = UI4ContainerScope(screenBox)
        scope.content()
        return root
    }

    fun screen(
        alignment: Alignment = Alignment.Center,
        modifier: Modifier = Modifier.fillMaxSize(),
        padding: Int,
        backgroundColor: Color? = null,
        background: Color? = null,
        color: Color? = null,
        content: UI4ContainerScope.() -> Unit
    ): UiRoot = screen(
        alignment = alignment,
        modifier = modifier,
        padding = padding.toFloat(),
        backgroundColor = backgroundColor ?: background ?: color,
        content = content
    )

    fun screen(
        background: String,
        alignment: Alignment = Alignment.Center,
        modifier: Modifier = Modifier.fillMaxSize(),
        padding: Float = 0f,
        content: UI4ContainerScope.() -> Unit
    ): UiRoot = screen(
        alignment = alignment,
        modifier = modifier,
        padding = padding,
        backgroundColor = Color.parse(background),
        content = content
    )

    fun screen(
        background: String,
        alignment: Alignment = Alignment.Center,
        modifier: Modifier = Modifier.fillMaxSize(),
        padding: Int,
        content: UI4ContainerScope.() -> Unit
    ): UiRoot = screen(
        alignment = alignment,
        modifier = modifier,
        padding = padding.toFloat(),
        backgroundColor = Color.parse(background),
        content = content
    )
}

/**
 * Container scope for nesting layout nodes and leaves (Phases 082–085).
 */
class UI4ContainerScope(val container: UiNode) {

    fun column(
        gap: Float = 0f,
        alignment: HorizontalAlign = HorizontalAlign.Center,
        modifier: Modifier = Modifier,
        padding: Float = 0f,
        backgroundColor: Color? = null,
        background: Color? = null,
        content: UI4ContainerScope.() -> Unit
    ): ColumnNode {
        var mod = modifier
        val bg = backgroundColor ?: background
        if (padding > 0f) mod = mod.padding(padding)
        if (bg != null) mod = mod.background(bg)
        val col = ColumnNode(gap = gap, horizontalAlignment = alignment).apply {
            this.modifier = mod
        }
        container.addChild(col)
        val scope = UI4ContainerScope(col)
        scope.content()
        return col
    }

    fun column(
        gap: Int,
        alignment: Alignment = Alignment.CenterHorizontally,
        modifier: Modifier = Modifier,
        padding: Int = 0,
        backgroundColor: Color? = null,
        background: Color? = null,
        content: UI4ContainerScope.() -> Unit
    ): ColumnNode = column(
        gap = gap.toFloat(),
        alignment = alignment.horizontal,
        modifier = modifier,
        padding = padding.toFloat(),
        backgroundColor = backgroundColor ?: background,
        content = content
    )

    fun column(
        alignment: Alignment,
        modifier: Modifier = Modifier,
        padding: Float = 0f,
        backgroundColor: Color? = null,
        background: Color? = null,
        content: UI4ContainerScope.() -> Unit
    ): ColumnNode = column(
        gap = 0f,
        alignment = alignment.horizontal,
        modifier = modifier,
        padding = padding,
        backgroundColor = backgroundColor ?: background,
        content = content
    )

    fun column(
        gap: Float,
        alignment: Alignment,
        modifier: Modifier = Modifier,
        padding: Float = 0f,
        backgroundColor: Color? = null,
        background: Color? = null,
        content: UI4ContainerScope.() -> Unit
    ): ColumnNode = column(
        gap = gap,
        alignment = alignment.horizontal,
        modifier = modifier,
        padding = padding,
        backgroundColor = backgroundColor ?: background,
        content = content
    )

    fun column(
        gap: Int,
        alignment: String,
        modifier: Modifier = Modifier,
        padding: Int = 0,
        backgroundColor: Color? = null,
        background: Color? = null,
        content: UI4ContainerScope.() -> Unit
    ): ColumnNode {
        val hAlign = when (alignment.lowercase()) {
            "start", "left" -> HorizontalAlign.Start
            "end", "right" -> HorizontalAlign.End
            else -> HorizontalAlign.Center
        }
        return column(
            gap = gap.toFloat(),
            alignment = hAlign,
            modifier = modifier,
            padding = padding.toFloat(),
            backgroundColor = backgroundColor ?: background,
            content = content
        )
    }

    fun row(
        gap: Float = 0f,
        alignment: VerticalAlign = VerticalAlign.Center,
        modifier: Modifier = Modifier,
        padding: Float = 0f,
        backgroundColor: Color? = null,
        background: Color? = null,
        content: UI4ContainerScope.() -> Unit
    ): RowNode {
        var mod = modifier
        val bg = backgroundColor ?: background
        if (padding > 0f) mod = mod.padding(padding)
        if (bg != null) mod = mod.background(bg)
        val rowNode = RowNode(gap = gap, verticalAlignment = alignment).apply {
            this.modifier = mod
        }
        container.addChild(rowNode)
        val scope = UI4ContainerScope(rowNode)
        scope.content()
        return rowNode
    }

    fun row(
        gap: Int,
        alignment: Alignment = Alignment.CenterVertically,
        modifier: Modifier = Modifier,
        padding: Int = 0,
        backgroundColor: Color? = null,
        background: Color? = null,
        content: UI4ContainerScope.() -> Unit
    ): RowNode = row(
        gap = gap.toFloat(),
        alignment = alignment.vertical,
        modifier = modifier,
        padding = padding.toFloat(),
        backgroundColor = backgroundColor ?: background,
        content = content
    )

    fun row(
        alignment: Alignment,
        modifier: Modifier = Modifier,
        padding: Float = 0f,
        backgroundColor: Color? = null,
        background: Color? = null,
        content: UI4ContainerScope.() -> Unit
    ): RowNode = row(
        gap = 0f,
        alignment = alignment.vertical,
        modifier = modifier,
        padding = padding,
        backgroundColor = backgroundColor ?: background,
        content = content
    )

    fun row(
        gap: Float,
        alignment: Alignment,
        modifier: Modifier = Modifier,
        padding: Float = 0f,
        backgroundColor: Color? = null,
        background: Color? = null,
        content: UI4ContainerScope.() -> Unit
    ): RowNode = row(
        gap = gap,
        alignment = alignment.vertical,
        modifier = modifier,
        padding = padding,
        backgroundColor = backgroundColor ?: background,
        content = content
    )

    fun row(
        gap: Int,
        alignment: String,
        modifier: Modifier = Modifier,
        padding: Int = 0,
        backgroundColor: Color? = null,
        background: Color? = null,
        content: UI4ContainerScope.() -> Unit
    ): RowNode {
        val vAlign = when (alignment.lowercase()) {
            "top" -> VerticalAlign.Top
            "bottom" -> VerticalAlign.Bottom
            else -> VerticalAlign.Center
        }
        return row(
            gap = gap.toFloat(),
            alignment = vAlign,
            modifier = modifier,
            padding = padding.toFloat(),
            backgroundColor = backgroundColor ?: background,
            content = content
        )
    }

    fun box(
        alignment: Alignment = Alignment.Center,
        modifier: Modifier = Modifier,
        padding: Float = 0f,
        backgroundColor: Color? = null,
        content: UI4ContainerScope.() -> Unit
    ): BoxNode {
        var mod = modifier
        if (padding > 0f) mod = mod.padding(padding)
        if (backgroundColor != null) mod = mod.background(backgroundColor)
        val boxNode = BoxNode(alignment = alignment).apply {
            this.modifier = mod
        }
        container.addChild(boxNode)
        val scope = UI4ContainerScope(boxNode)
        scope.content()
        return boxNode
    }

    fun box(
        padding: Int,
        backgroundColor: Color? = null,
        alignment: Alignment = Alignment.Center,
        modifier: Modifier = Modifier,
        content: UI4ContainerScope.() -> Unit
    ): BoxNode = box(alignment, modifier, padding.toFloat(), backgroundColor, content)

    fun center(
        modifier: Modifier = Modifier.fillMaxSize(),
        content: UI4ContainerScope.() -> Unit
    ): BoxNode = box(alignment = Alignment.Center, modifier = modifier, content = content)

    fun stack(
        alignment: Alignment = Alignment.TopStart,
        modifier: Modifier = Modifier,
        content: UI4ContainerScope.() -> Unit
    ): StackNode {
        val stackNode = StackNode(alignment = alignment).apply {
            this.modifier = modifier
        }
        container.addChild(stackNode)
        val scope = UI4ContainerScope(stackNode)
        scope.content()
        return stackNode
    }

    fun text(
        value: String,
        modifier: Modifier = Modifier,
        style: TextStyle = TextStyle.Body,
        color: Color? = null
    ): TextNode {
        val effectiveStyle = if (color != null) style.copy(color = color) else style
        val node = TextNode(text = value, style = effectiveStyle).apply {
            this.modifier = modifier
        }
        container.addChild(node)
        return node
    }

    fun text(
        state: State<*>,
        modifier: Modifier = Modifier,
        style: TextStyle = TextStyle.Body,
        color: Color? = null
    ): TextNode {
        val effectiveStyle = if (color != null) style.copy(color = color) else style
        val node = TextNode(text = state.value.toString(), style = effectiveStyle).apply {
            this.modifier = modifier
        }
        node.bindText(state)
        container.addChild(node)
        return node
    }

    fun button(
        label: String = "",
        text: String = label,
        modifier: Modifier = Modifier,
        backgroundColor: Color? = null,
        textColor: Color? = null,
        enabled: Boolean = true,
        onClick: () -> Unit = {}
    ): ButtonNode {
        val actualLabel = if (text.isNotEmpty()) text else label
        var mod = modifier
        if (backgroundColor != null) {
            mod = mod.background(backgroundColor)
        }
        val node = ButtonNode(label = actualLabel, onClick = onClick).apply {
            this.modifier = mod
            this.enabled = enabled
            this.backgroundColor = backgroundColor
            this.textColor = textColor
        }
        container.addChild(node)
        return node
    }

    fun scroll(
        state: ScrollState = ScrollState(),
        modifier: Modifier = Modifier,
        content: UI4ContainerScope.() -> Unit
    ): ScrollNode {
        val node = ScrollNode(state).apply {
            this.modifier = modifier
        }
        container.addChild(node)
        val scope = UI4ContainerScope(node)
        scope.content()
        return node
    }

    fun textField(
        text: String = "",
        placeholder: String = "Enter text...",
        modifier: Modifier = Modifier,
        onValueChanged: (String) -> Unit = {}
    ): TextFieldNode {
        val node = TextFieldNode(text, placeholder, onValueChanged).apply {
            this.modifier = modifier
        }
        container.addChild(node)
        return node
    }

    fun textField(
        state: MutableState<String>,
        placeholder: String = "Enter text...",
        modifier: Modifier = Modifier
    ): TextFieldNode {
        val node = TextFieldNode(
            text = state.value,
            placeholder = placeholder,
            onValueChanged = { state.value = it }
        ).apply {
            this.modifier = modifier
        }
        container.addChild(node)
        return node
    }
}

/**
 * Main application entry DSL function (Phases 080, 090).
 */
fun app(block: UI4AppScope.() -> Unit): UiRoot {
    val appScope = UI4AppScope()
    appScope.block()
    return appScope.root
}

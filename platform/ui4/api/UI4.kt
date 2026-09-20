package ui4

import ui4.core.Alignment
import ui4.core.Color
import ui4.core.Constraints
import ui4.core.HorizontalAlign
import ui4.core.Insets
import ui4.core.Modifier
import ui4.core.Point
import ui4.core.Rect
import ui4.core.Size
import ui4.core.VerticalAlign
import ui4.core.fillMaxHeight
import ui4.core.fillMaxSize
import ui4.core.fillMaxWidth
import ui4.core.padding
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

/**
 * Mutable state container (Phase 045).
 */
class State<T>(var value: T)

fun <T> state(initial: T): State<T> = State(initial)

/**
 * Top-level application scope for building UI trees (Phases 080, 081).
 */
class UI4AppScope {
    val root: UiRoot = UiRoot()

    fun screen(
        modifier: Modifier = Modifier.fillMaxSize(),
        content: UI4ContainerScope.() -> Unit
    ): UiRoot {
        val screenBox = BoxNode(alignment = Alignment.Center).apply {
            this.modifier = modifier
            this.tag = "Screen"
        }
        root.rootChild = screenBox

        val scope = UI4ContainerScope(screenBox)
        scope.content()
        return root
    }
}

/**
 * Container scope for nesting layout nodes and leaves (Phases 082–085).
 */
class UI4ContainerScope(val container: UiNode) {

    fun column(
        gap: Float = 0f,
        alignment: HorizontalAlign = HorizontalAlign.Center,
        modifier: Modifier = Modifier,
        content: UI4ContainerScope.() -> Unit
    ): ColumnNode {
        val col = ColumnNode(gap = gap, horizontalAlignment = alignment).apply {
            this.modifier = modifier
        }
        container.addChild(col)
        val scope = UI4ContainerScope(col)
        scope.content()
        return col
    }

    fun column(
        gap: Int,
        alignment: String = "center",
        modifier: Modifier = Modifier,
        content: UI4ContainerScope.() -> Unit
    ): ColumnNode {
        val hAlign = when (alignment.lowercase()) {
            "start", "left" -> HorizontalAlign.Start
            "end", "right" -> HorizontalAlign.End
            else -> HorizontalAlign.Center
        }
        return column(gap.toFloat(), hAlign, modifier, content)
    }

    fun row(
        gap: Float = 0f,
        alignment: VerticalAlign = VerticalAlign.Center,
        modifier: Modifier = Modifier,
        content: UI4ContainerScope.() -> Unit
    ): RowNode {
        val rowNode = RowNode(gap = gap, verticalAlignment = alignment).apply {
            this.modifier = modifier
        }
        container.addChild(rowNode)
        val scope = UI4ContainerScope(rowNode)
        scope.content()
        return rowNode
    }

    fun row(
        gap: Int,
        alignment: String = "center",
        modifier: Modifier = Modifier,
        content: UI4ContainerScope.() -> Unit
    ): RowNode {
        val vAlign = when (alignment.lowercase()) {
            "top" -> VerticalAlign.Top
            "bottom" -> VerticalAlign.Bottom
            else -> VerticalAlign.Center
        }
        return row(gap.toFloat(), vAlign, modifier, content)
    }

    fun box(
        alignment: Alignment = Alignment.Center,
        modifier: Modifier = Modifier,
        content: UI4ContainerScope.() -> Unit
    ): BoxNode {
        val boxNode = BoxNode(alignment = alignment).apply {
            this.modifier = modifier
        }
        container.addChild(boxNode)
        val scope = UI4ContainerScope(boxNode)
        scope.content()
        return boxNode
    }

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
        style: TextStyle = TextStyle.Body
    ): TextNode {
        val node = TextNode(text = value, style = style).apply {
            this.modifier = modifier
        }
        container.addChild(node)
        return node
    }

    fun button(
        label: String,
        modifier: Modifier = Modifier,
        onClick: () -> Unit = {}
    ): ButtonNode {
        val node = ButtonNode(label = label, onClick = onClick).apply {
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

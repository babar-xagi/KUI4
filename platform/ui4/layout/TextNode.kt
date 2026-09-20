package ui4.layout

import ui4.core.Constraints
import ui4.core.Insets
import ui4.core.NodeId
import ui4.core.NodeIdGenerator
import ui4.core.Point
import ui4.core.Rect
import ui4.core.Size
import ui4.core.findPadding
import ui4.tree.UiNode

/**
 * Text typography styles.
 */
enum class TextStyle(val fontSize: Float) {
    Headline(28f),
    Title(20f),
    Body(14f),
    Caption(11f)
}

/**
 * Leaf UI node for rendering text (Phase 085).
 */
class TextNode(
    var text: String,
    var style: TextStyle = TextStyle.Body,
    id: NodeId = NodeIdGenerator.next()
) : UiNode(id, "Text") {

    override fun measure(constraints: Constraints): Size {
        incomingConstraints = constraints
        val padding = modifier.findPadding() ?: Insets.Zero

        // Intrinsic character-based size estimation
        val charWidth = style.fontSize * 0.58f
        val lineHeight = style.fontSize * 1.35f

        val estimatedW = (text.length * charWidth) + padding.horizontal
        val estimatedH = lineHeight + padding.vertical

        measuredSize = constraints.clamp(Size(estimatedW, estimatedH))
        return measuredSize
    }

    override fun layout(origin: Point, finalSize: Size) {
        bounds = Rect.fromOriginAndSize(origin, finalSize)
        dirty = false
    }

    override fun toString(): String = "Text$id \"$text\" $bounds"
}

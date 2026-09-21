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
 * Text typography styles supporting both preset constants and custom configurations.
 */
data class TextStyle(
    val fontSize: Float = 14f,
    val fontWeight: ui4.text.FontWeight = ui4.text.FontWeight.Normal,
    val color: ui4.core.Color? = null,
    val fontFamily: String = "System",
    val textAlign: ui4.text.TextAlign = ui4.text.TextAlign.Start
) {
    constructor(
        fontSize: Int,
        fontWeight: ui4.text.FontWeight = ui4.text.FontWeight.Normal,
        color: ui4.core.Color? = null,
        fontFamily: String = "System",
        textAlign: ui4.text.TextAlign = ui4.text.TextAlign.Start
    ) : this(fontSize.toFloat(), fontWeight, color, fontFamily, textAlign)

    fun copy(fontSize: Int): TextStyle = copy(fontSize = fontSize.toFloat())

    companion object {
        val Headline = TextStyle(fontSize = 28f, fontWeight = ui4.text.FontWeight.Bold)
        val Title = TextStyle(fontSize = 20f, fontWeight = ui4.text.FontWeight.Medium)
        val Body = TextStyle(fontSize = 14f, fontWeight = ui4.text.FontWeight.Normal)
        val Caption = TextStyle(fontSize = 11f, fontWeight = ui4.text.FontWeight.Normal)
    }
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

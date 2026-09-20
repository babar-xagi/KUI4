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
 * Clickable interactive button node (Phase 085).
 */
class ButtonNode(
    var label: String,
    onClick: () -> Unit = {},
    id: NodeId = NodeIdGenerator.next()
) : UiNode(id, "Button") {

    constructor(label: String, onClick: () -> Unit) : this(label, onClick, NodeIdGenerator.next())

    init {
        this.onClick = onClick
        this.isFocusable = true
    }

    fun click() {
        if (enabled) {
            onClick?.invoke()
        }
    }

    override fun measure(constraints: Constraints): Size {
        incomingConstraints = constraints
        val modifierPadding = modifier.findPadding() ?: Insets.Zero
        val defaultButtonPadding = Insets.symmetric(horizontal = 16f, vertical = 10f)

        val totalPadding = Insets(
            left = modifierPadding.left + defaultButtonPadding.left,
            top = modifierPadding.top + defaultButtonPadding.top,
            right = modifierPadding.right + defaultButtonPadding.right,
            bottom = modifierPadding.bottom + defaultButtonPadding.bottom
        )

        val charWidth = 14f * 0.58f
        val textWidth = label.length * charWidth
        val textHeight = 14f * 1.35f

        val desiredW = textWidth + totalPadding.horizontal
        val desiredH = maxOf(40f, textHeight + totalPadding.vertical) // min 40dp touch target

        measuredSize = constraints.clamp(Size(desiredW, desiredH))
        return measuredSize
    }

    override fun layout(origin: Point, finalSize: Size) {
        bounds = Rect.fromOriginAndSize(origin, finalSize)
        dirty = false
    }

    override fun toString(): String = "Button$id \"$label\" $bounds"
}

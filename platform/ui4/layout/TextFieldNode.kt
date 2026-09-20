package ui4.layout

import ui4.core.Color
import ui4.core.Constraints
import ui4.core.Insets
import ui4.core.NodeId
import ui4.core.NodeIdGenerator
import ui4.core.Point
import ui4.core.Rect
import ui4.core.Size
import ui4.core.findPadding
import ui4.text.TextRange
import ui4.tree.UiNode
import ui4.tree.markDirty

/**
 * Interactive single-line editable text input component (Phases 142–146).
 */
class TextFieldNode(
    var text: String = "",
    var placeholder: String = "Enter text...",
    var onValueChanged: (String) -> Unit = {},
    id: NodeId = NodeIdGenerator.next()
) : UiNode(id, "TextField") {

    init {
        isFocusable = true
    }

    var cursorIndex: Int = text.length
        private set

    var selection: TextRange = TextRange.collapsed(text.length)
        private set

    var isImeVisible: Boolean = false
        internal set

    var fontSize: Float = 14f
    val charWidth: Float get() = fontSize * 0.58f

    override fun measure(constraints: Constraints): Size {
        incomingConstraints = constraints
        val pad = modifier.findPadding() ?: Insets.symmetric(horizontal = 12f, vertical = 8f)
        val displayLength = maxOf(text.length, placeholder.length, 10)
        val desiredW = (displayLength * charWidth) + pad.horizontal
        val desiredH = maxOf(44f, (fontSize * 1.35f) + pad.vertical) // 44dp standard touch target

        measuredSize = constraints.clamp(Size(desiredW, desiredH))
        return measuredSize
    }

    override fun layout(origin: Point, finalSize: Size) {
        bounds = Rect.fromOriginAndSize(origin, finalSize)
        dirty = false
    }

    /**
     * Commits typed or pasted text at the current cursor/selection position (Phase 144).
     */
    fun commitText(input: String) {
        if (!enabled) return

        val newText = if (!selection.isCollapsed) {
            selection.replace(text, input)
        } else {
            val idx = cursorIndex.coerceIn(0, text.length)
            text.substring(0, idx) + input + text.substring(idx)
        }

        val newCursor = if (!selection.isCollapsed) {
            selection.min + input.length
        } else {
            cursorIndex + input.length
        }

        text = newText
        cursorIndex = newCursor.coerceIn(0, text.length)
        selection = TextRange.collapsed(cursorIndex)

        onValueChanged(text)
        markDirty()
    }

    /**
     * Deletes the character preceding the cursor or the active selection (Phase 144).
     */
    fun deleteBackward() {
        if (!enabled || text.isEmpty()) return

        if (!selection.isCollapsed) {
            text = selection.replace(text, "")
            cursorIndex = selection.min.coerceIn(0, text.length)
            selection = TextRange.collapsed(cursorIndex)
        } else if (cursorIndex > 0) {
            val idx = cursorIndex.coerceIn(1, text.length)
            text = text.substring(0, idx - 1) + text.substring(idx)
            cursorIndex = idx - 1
            selection = TextRange.collapsed(cursorIndex)
        }

        onValueChanged(text)
        markDirty()
    }

    /**
     * Positions cursor at a specific character offset (Phase 145).
     */
    fun setCursor(index: Int) {
        cursorIndex = index.coerceIn(0, text.length)
        selection = TextRange.collapsed(cursorIndex)
        markDirty()
    }

    /**
     * Selects all text content (Phase 146).
     */
    fun selectAll() {
        selection = TextRange(0, text.length)
        cursorIndex = text.length
        markDirty()
    }

    /**
     * Computes the bounding rectangle of the caret cursor for rendering (Phase 145).
     */
    fun calculateCursorRect(): Rect {
        val pad = modifier.findPadding() ?: Insets.symmetric(horizontal = 12f, vertical = 8f)
        val x = bounds.left + pad.left + (cursorIndex * charWidth)
        val y = bounds.top + pad.top
        val h = fontSize * 1.25f
        return Rect(x, y, 2f, h)
    }

    /**
     * Computes the selection highlight rectangle (Phase 146).
     */
    fun calculateSelectionRect(): Rect? {
        if (selection.isCollapsed) return null
        val pad = modifier.findPadding() ?: Insets.symmetric(horizontal = 12f, vertical = 8f)
        val x = bounds.left + pad.left + (selection.min * charWidth)
        val y = bounds.top + pad.top
        val w = selection.length * charWidth
        val h = fontSize * 1.25f
        return Rect(x, y, w, h)
    }

    override fun toString(): String =
        "TextField$id \"$text\" cursor=$cursorIndex selection=$selection $bounds"
}

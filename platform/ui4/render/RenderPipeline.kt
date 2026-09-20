package ui4.render

import ui4.core.Color
import ui4.core.Insets
import ui4.core.Point
import ui4.core.findAlpha
import ui4.core.findBackground
import ui4.core.findClip
import ui4.core.findPadding
import ui4.core.findRadius
import ui4.layout.ButtonNode
import ui4.layout.TextStyle
import ui4.layout.TextNode
import ui4.tree.UiNode
import ui4.tree.UiRoot
import ui4.tree.cleanDirty

/**
 * Result metrics for a completed render pass (Phases 096, 100).
 */
data class RenderResult(
    val rendered: Boolean,
    val opCount: Int,
    val durationNanos: Long
) {
    val durationMs: Double
        get() = durationNanos / 1_000_000.0
}

/**
 * Two-pass rendering pipeline converting UI4 tree into structured draw operations (Phases 093–100).
 */
class RenderPipeline {

    /**
     * Executes a render pass. Honors invalidation by skipping if tree is clean (Phase 096).
     */
    fun render(root: UiRoot, canvas: UiCanvas, force: Boolean = false): RenderResult {
        if (!force && !root.isDirty) {
            // Idle: No nodes are dirty, skip draw pass entirely (Phase 096)
            return RenderResult(rendered = false, opCount = 0, durationNanos = 0L)
        }

        val startNanos = System.nanoTime()
        val child = root.rootChild
        if (child != null) {
            renderNode(child, canvas)
        }
        val endNanos = System.nanoTime()

        // Invalidate dirty flags down the tree
        root.cleanDirty()

        val totalOps = if (canvas is RecordingCanvas) canvas.ops.size else 0
        return RenderResult(
            rendered = true,
            opCount = totalOps,
            durationNanos = endNanos - startNanos
        )
    }

    /**
     * Recursively renders a single UI node and its children (Phases 093–099).
     */
    fun renderNode(node: UiNode, canvas: UiCanvas) {
        val alpha = node.modifier.findAlpha()
        val clip = node.modifier.findClip() ?: false
        val needsSave = (alpha != null && alpha < 1.0f) || clip

        var saveCount = 0
        if (needsSave) {
            saveCount = canvas.save()
            if (alpha != null && alpha < 1.0f) {
                canvas.setAlpha(alpha)
            }
            if (clip) {
                canvas.clipRect(node.bounds)
            }
        }

        // 1. Background rendering (Phases 093, 098)
        val bg = node.modifier.findBackground()
        val radius = node.modifier.findRadius() ?: 0f
        if (bg != null) {
            if (radius > 0f) {
                canvas.drawRoundRect(node.bounds, radius, bg)
            } else {
                canvas.drawRect(node.bounds, bg)
            }
        }

        // 2. Node-specific visual element rendering (Phases 093, 094, 110, 111)
        when (node) {
            is ButtonNode -> {
                renderButton(node, canvas, bg, radius)
            }
            is TextNode -> {
                renderText(node, canvas)
            }
        }

        // 3. Recursive child traversal in Z-order (Phase 095)
        for (child in node.children) {
            renderNode(child, canvas)
        }

        // Restore canvas state
        if (needsSave) {
            canvas.restoreToCount(saveCount)
        }
    }

    private fun renderButton(
        button: ButtonNode,
        canvas: UiCanvas,
        explicitBg: Color?,
        explicitRadius: Float
    ) {
        val radius = if (explicitRadius > 0f) explicitRadius else 6f

        // If no explicit background modifier, apply default button theme
        if (explicitBg == null) {
            val buttonBg = when {
                !button.enabled -> Color(0.85f, 0.85f, 0.85f)
                button.isPressed -> Color(0.12f, 0.42f, 0.85f) // Darker pressed blue
                button.isFocused -> Color(0.20f, 0.55f, 1.0f)  // Focus highlight
                else -> Color(0.15f, 0.48f, 0.95f)            // Normal button blue
            }
            canvas.drawRoundRect(button.bounds, radius, buttonBg)
        }

        // Button label centered
        val textColor = if (button.enabled) Color.White else Color(0.55f, 0.55f, 0.55f)
        val charWidth = 14f * 0.58f
        val textW = button.label.length * charWidth
        val textH = 14f * 1.35f

        val textX = button.bounds.left + (button.bounds.width - textW) / 2f
        val textY = button.bounds.top + (button.bounds.height - textH) / 2f
        canvas.drawText(
            text = button.label,
            x = textX,
            y = textY,
            color = textColor,
            style = TextStyle.Body,
            fontSize = 14f
        )
    }

    private fun renderText(textNode: TextNode, canvas: UiCanvas) {
        val padding = textNode.modifier.findPadding() ?: Insets.Zero
        val textX = textNode.bounds.left + padding.left
        val textY = textNode.bounds.top + padding.top
        val textColor = if (textNode.enabled) Color.Black else Color(0.55f, 0.55f, 0.55f)

        canvas.drawText(
            text = textNode.text,
            x = textX,
            y = textY,
            color = textColor,
            style = textNode.style,
            fontSize = textNode.style.fontSize
        )
    }
}

package ui4.render

import ui4.core.Color
import ui4.core.Point
import ui4.core.Rect
import ui4.layout.TextStyle

/**
 * Structured drawing commands emitted by the UI4 rendering engine (Phases 092–099).
 */
sealed class RenderOp {
    data class DrawRect(val rect: Rect, val color: Color) : RenderOp()
    data class DrawRoundRect(val rect: Rect, val radius: Float, val color: Color) : RenderOp()
    data class DrawText(
        val text: String,
        val x: Float,
        val y: Float,
        val color: Color,
        val style: TextStyle,
        val fontSize: Float
    ) : RenderOp()
    data class ClipRect(val rect: Rect) : RenderOp()
    data class SetAlpha(val alpha: Float) : RenderOp()
    data class Save(val saveCount: Int) : RenderOp()
    data class Restore(val targetCount: Int) : RenderOp()
    data class Translate(val dx: Float, val dy: Float) : RenderOp()
}

/**
 * Pure Kotlin Canvas surface contract for 2D drawing (Phase 092).
 * Operates across desktop simulation, test recorders, and Android platform surfaces.
 */
interface UiCanvas {

    val currentAlpha: Float
    val clipBounds: Rect?

    /**
     * Draws an axis-aligned rectangle (Phase 093).
     */
    fun drawRect(rect: Rect, color: Color)

    /**
     * Draws a rectangle with rounded corners (Phase 098).
     */
    fun drawRoundRect(rect: Rect, radius: Float, color: Color)

    /**
     * Draws text at the specified baseline position (Phase 094).
     */
    fun drawText(
        text: String,
        x: Float,
        y: Float,
        color: Color = Color.Black,
        style: TextStyle = TextStyle.Body,
        fontSize: Float = style.fontSize
    )

    /**
     * Clips subsequent drawing operations to the specified rectangle (Phase 097).
     */
    fun clipRect(rect: Rect)

    /**
     * Sets the composite layer opacity in [0.0, 1.0] (Phase 099).
     */
    fun setAlpha(alpha: Float)

    /**
     * Translates the canvas coordinate space.
     */
    fun translate(dx: Float, dy: Float)

    /**
     * Saves the current canvas state (clipping, alpha, matrix).
     * @return current save count.
     */
    fun save(): Int

    /**
     * Restores the most recently saved canvas state.
     */
    fun restore()

    /**
     * Restores state to a specific save count.
     */
    fun restoreToCount(count: Int)
}

/**
 * In-memory recording canvas that captures all render commands (Phase 092, 095).
 * Enables fast headless execution, automated verification, and snapshot tests.
 */
class RecordingCanvas : UiCanvas {

    val ops: MutableList<RenderOp> = mutableListOf()

    private val alphaStack: MutableList<Float> = mutableListOf(1.0f)
    private val clipStack: MutableList<Rect?> = mutableListOf(null)
    private var saveCounter: Int = 1

    override val currentAlpha: Float
        get() = alphaStack.last()

    override val clipBounds: Rect?
        get() = clipStack.last()

    override fun drawRect(rect: Rect, color: Color) {
        val effectiveAlpha = currentAlpha * color.alpha
        val effectiveColor = if (effectiveAlpha < 1.0f) {
            Color(color.red, color.green, color.blue, effectiveAlpha)
        } else {
            color
        }
        ops.add(RenderOp.DrawRect(rect, effectiveColor))
    }

    override fun drawRoundRect(rect: Rect, radius: Float, color: Color) {
        val effectiveAlpha = currentAlpha * color.alpha
        val effectiveColor = if (effectiveAlpha < 1.0f) {
            Color(color.red, color.green, color.blue, effectiveAlpha)
        } else {
            color
        }
        ops.add(RenderOp.DrawRoundRect(rect, radius, effectiveColor))
    }

    override fun drawText(
        text: String,
        x: Float,
        y: Float,
        color: Color,
        style: TextStyle,
        fontSize: Float
    ) {
        val effectiveAlpha = currentAlpha * color.alpha
        val effectiveColor = if (effectiveAlpha < 1.0f) {
            Color(color.red, color.green, color.blue, effectiveAlpha)
        } else {
            color
        }
        ops.add(RenderOp.DrawText(text, x, y, effectiveColor, style, fontSize))
    }

    override fun clipRect(rect: Rect) {
        val current = clipBounds
        val intersected = if (current != null) current.intersect(rect) else rect
        clipStack[clipStack.lastIndex] = intersected
        ops.add(RenderOp.ClipRect(rect))
    }

    override fun setAlpha(alpha: Float) {
        val clamped = alpha.coerceIn(0f, 1f)
        val composite = (if (alphaStack.size > 1) alphaStack[alphaStack.size - 2] else 1.0f) * clamped
        alphaStack[alphaStack.lastIndex] = composite
        ops.add(RenderOp.SetAlpha(composite))
    }

    override fun translate(dx: Float, dy: Float) {
        ops.add(RenderOp.Translate(dx, dy))
    }

    override fun save(): Int {
        val count = saveCounter++
        alphaStack.add(currentAlpha)
        clipStack.add(clipBounds)
        ops.add(RenderOp.Save(count))
        return count
    }

    override fun restore() {
        if (alphaStack.size > 1) {
            alphaStack.removeAt(alphaStack.lastIndex)
        }
        if (clipStack.size > 1) {
            clipStack.removeAt(clipStack.lastIndex)
        }
        ops.add(RenderOp.Restore(alphaStack.size))
    }

    override fun restoreToCount(count: Int) {
        while (alphaStack.size > count && alphaStack.size > 1) {
            alphaStack.removeAt(alphaStack.lastIndex)
            clipStack.removeAt(clipStack.lastIndex)
        }
        ops.add(RenderOp.Restore(count))
    }

    fun clear() {
        ops.clear()
        alphaStack.clear()
        alphaStack.add(1.0f)
        clipStack.clear()
        clipStack.add(null)
        saveCounter = 1
    }

    inline fun <reified T : RenderOp> opsOfType(): List<T> =
        ops.filterIsInstance<T>()

    fun snapshot(): List<RenderOp> = ops.toList()
}

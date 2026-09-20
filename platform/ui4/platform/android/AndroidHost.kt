package ui4.platform.android

import ui4.core.Point
import ui4.input.FocusManager
import ui4.input.InputManager
import ui4.input.KeyEvent
import ui4.input.PointerEvent
import ui4.input.PointerEventType
import ui4.render.RecordingCanvas
import ui4.render.RenderPipeline
import ui4.render.RenderResult
import ui4.render.UiCanvas
import ui4.tree.UiRoot

/**
 * Universal application hosting surface contract (Phase 091).
 * Bridges the UI4 runtime to host windows, Android Activities/Views, and headless test runners.
 */
interface UiHost {

    val isAttached: Boolean
    val currentRoot: UiRoot?

    /**
     * Attaches a UI4 root hierarchy to this host surface (Phase 091).
     */
    fun attach(root: UiRoot)

    /**
     * Detaches the active root.
     */
    fun detach()

    /**
     * Requests an invalidation / redraw pass.
     */
    fun requestRender()

    /**
     * Handles surface or window viewport resizing.
     */
    fun onViewportChanged(width: Float, height: Float)

    /**
     * Forwards a pointer event to the active UI hierarchy (Phase 107–111).
     */
    fun dispatchPointer(event: PointerEvent): Boolean

    /**
     * Forwards a keyboard event to the active UI hierarchy (Phase 113, 114).
     */
    fun dispatchKey(event: KeyEvent): Boolean
}

/**
 * Headless virtual host for testing and verification (Phases 091–115).
 * Simulates an Android device display surface with full layout, render, and input pipelines.
 */
class VirtualHost(
    var viewportWidth: Float = 1080f,
    var viewportHeight: Float = 1920f
) : UiHost {

    override var isAttached: Boolean = false
        private set

    override var currentRoot: UiRoot? = null
        private set

    val canvas: RecordingCanvas = RecordingCanvas()
    val pipeline: RenderPipeline = RenderPipeline()

    var focusManager: FocusManager? = null
        private set

    var inputManager: InputManager? = null
        private set

    override fun attach(root: UiRoot) {
        currentRoot = root
        isAttached = true
        val rootNode = root.rootChild ?: root
        val fm = FocusManager(rootNode)
        focusManager = fm
        inputManager = InputManager(rootNode, fm)

        // Perform initial layout pass
        root.performLayout(viewportWidth, viewportHeight)
    }

    override fun detach() {
        isAttached = false
        currentRoot = null
        focusManager = null
        inputManager = null
    }

    override fun requestRender() {
        val root = currentRoot ?: return
        if (root.isDirty) {
            root.performLayout(viewportWidth, viewportHeight)
        }
        pipeline.render(root, canvas)
    }

    override fun onViewportChanged(width: Float, height: Float) {
        viewportWidth = width
        viewportHeight = height
        currentRoot?.performLayout(viewportWidth, viewportHeight)
    }

    override fun dispatchPointer(event: PointerEvent): Boolean {
        val mgr = inputManager ?: return false
        val consumed = mgr.dispatchPointer(event)
        if (currentRoot?.isDirty == true) {
            requestRender()
        }
        return consumed
    }

    override fun dispatchKey(event: KeyEvent): Boolean {
        val mgr = inputManager ?: return false
        val consumed = mgr.dispatchKey(event)
        if (currentRoot?.isDirty == true) {
            requestRender()
        }
        return consumed
    }

    /**
     * Executes an explicit render pass and returns the result metrics.
     */
    fun render(force: Boolean = false): RenderResult {
        val root = currentRoot ?: return RenderResult(false, 0, 0L)
        if (root.isDirty) {
            root.performLayout(viewportWidth, viewportHeight)
        }
        return pipeline.render(root, canvas, force)
    }

    // -------------------------------------------------------------
    // Testing and Automation Helpers
    // -------------------------------------------------------------

    fun click(x: Float, y: Float): Boolean {
        val pt = Point(x, y)
        val down = dispatchPointer(PointerEvent(PointerEventType.Down, pt))
        val up = dispatchPointer(PointerEvent(PointerEventType.Up, pt))
        return down || up
    }

    fun press(x: Float, y: Float): Boolean =
        dispatchPointer(PointerEvent(PointerEventType.Down, Point(x, y)))

    fun release(x: Float, y: Float): Boolean =
        dispatchPointer(PointerEvent(PointerEventType.Up, Point(x, y)))

    fun sendKey(code: ui4.input.KeyCode, action: ui4.input.KeyAction = ui4.input.KeyAction.Down): Boolean =
        dispatchKey(KeyEvent(code, action))
}

/**
 * Architectural bridge contract for Android Activity and SurfaceView hosting (Phases 091, 092).
 */
class AndroidHostBridge(
    val host: UiHost
) {
    /**
     * Invoked by Android View.onDraw(android.graphics.Canvas).
     */
    fun onPlatformDraw(platformCanvas: Any) {
        host.requestRender()
    }

    /**
     * Invoked by Android View.onTouchEvent(android.view.MotionEvent).
     */
    fun onPlatformTouchEvent(action: Int, x: Float, y: Float): Boolean {
        val eventType = when (action) {
            0 -> PointerEventType.Down
            2 -> PointerEventType.Move
            1 -> PointerEventType.Up
            3 -> PointerEventType.Cancel
            else -> return false
        }
        return host.dispatchPointer(PointerEvent(eventType, Point(x, y)))
    }

    /**
     * Invoked by Android View.onKeyDown / onKeyUp.
     */
    fun onPlatformKeyEvent(keyCode: Int, isDown: Boolean): Boolean {
        val code = when (keyCode) {
            66 -> ui4.input.KeyCode.Enter // KEYCODE_ENTER
            62 -> ui4.input.KeyCode.Space // KEYCODE_SPACE
            61 -> ui4.input.KeyCode.Tab   // KEYCODE_TAB
            111 -> ui4.input.KeyCode.Escape
            19 -> ui4.input.KeyCode.ArrowUp
            20 -> ui4.input.KeyCode.ArrowDown
            21 -> ui4.input.KeyCode.ArrowLeft
            22 -> ui4.input.KeyCode.ArrowRight
            else -> ui4.input.KeyCode.Other
        }
        val action = if (isDown) ui4.input.KeyAction.Down else ui4.input.KeyAction.Up
        return host.dispatchKey(KeyEvent(code, action))
    }
}

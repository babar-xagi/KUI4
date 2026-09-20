package tests

import ui4.app
import ui4.core.Alignment
import ui4.core.Color
import ui4.core.Insets
import ui4.core.Modifier
import ui4.core.Point
import ui4.core.Rect
import ui4.core.Size
import ui4.core.alpha
import ui4.core.background
import ui4.core.clip
import ui4.core.fillMaxSize
import ui4.core.findAlpha
import ui4.core.findClip
import ui4.core.findRadius
import ui4.core.padding
import ui4.core.rounded
import ui4.core.size
import ui4.gesture.GestureState
import ui4.gesture.TapGestureRecognizer
import ui4.input.FocusManager
import ui4.input.HitTest
import ui4.input.InputManager
import ui4.input.KeyAction
import ui4.input.KeyCode
import ui4.input.KeyEvent
import ui4.input.PointerEvent
import ui4.input.PointerEventType
import ui4.layout.BoxNode
import ui4.layout.ButtonNode
import ui4.layout.ColumnNode
import ui4.layout.RowNode
import ui4.layout.TextStyle
import ui4.layout.TextNode
import ui4.platform.android.AndroidHostBridge
import ui4.platform.android.VirtualHost
import ui4.render.RecordingCanvas
import ui4.render.RenderBenchmark
import ui4.render.RenderOp
import ui4.render.RenderPipeline
import ui4.state.bindState
import ui4.state.bindText
import ui4.state.mutableStateOf
import ui4.state.state
import ui4.tree.UiNode
import ui4.tree.UiRoot
import ui4.tree.addChild
import ui4.tree.cleanDirty
import ui4.tree.markDirty

/**
 * Automated test suite for Milestone E: Android Render + State + Input (Phases 091–115).
 */
fun main() {
    println("==================================================")
    println(" KUI4 Milestone E - Complete Test Suite (091-115)")
    println("==================================================")

    var passed = 0
    var failed = 0

    fun check(label: String, condition: Boolean) {
        if (condition) {
            println("  [PASS] $label")
            passed++
        } else {
            println("  [FAIL] $label")
            failed++
        }
    }

    // -------------------------------------------------------------
    // Phase 091: Android Host & Lifecycle
    // -------------------------------------------------------------
    println("\n>>> Running Phase 091: Android Host Tests...")
    val root91 = UiRoot()
    val box91 = BoxNode()
    root91.rootChild = box91
    val host91 = VirtualHost(viewportWidth = 800f, viewportHeight = 600f)

    check("Host is initially unattached", !host91.isAttached)
    host91.attach(root91)
    check("Host is attached after attach()", host91.isAttached)
    check("Root bounds laid out to viewport width", root91.bounds.width == 800f)
    check("Root bounds laid out to viewport height", root91.bounds.height == 600f)

    host91.onViewportChanged(400f, 300f)
    check("Root resized on viewport changed", root91.bounds.width == 400f && root91.bounds.height == 300f)

    val bridge91 = AndroidHostBridge(host91)
    check("Bridge created for platform integration", bridge91.host === host91)

    host91.detach()
    check("Host is unattached after detach()", !host91.isAttached)

    // -------------------------------------------------------------
    // Phase 092: Canvas Surface & RecordingCanvas
    // -------------------------------------------------------------
    println("\n>>> Running Phase 092: Canvas Surface Tests...")
    val canvas92 = RecordingCanvas()
    check("Initial canvas ops empty", canvas92.ops.isEmpty())
    check("Initial alpha is 1.0", canvas92.currentAlpha == 1.0f)

    val saveCount = canvas92.save()
    check("Save count increments", saveCount >= 1)
    canvas92.setAlpha(0.5f)
    check("Alpha updated", canvas92.currentAlpha == 0.5f)
    canvas92.restore()
    check("Alpha restored to 1.0", canvas92.currentAlpha == 1.0f)

    // -------------------------------------------------------------
    // Phase 093: Rectangle Render
    // -------------------------------------------------------------
    println("\n>>> Running Phase 093: Rectangle Render Tests...")
    canvas92.clear()
    val rect93 = Rect(10f, 20f, 100f, 50f)
    val color93 = Color(0xFF112233L)
    canvas92.drawRect(rect93, color93)

    val rectOps = canvas92.opsOfType<RenderOp.DrawRect>()
    check("Emitted DrawRect operation", rectOps.size == 1)
    check("DrawRect rect matches", rectOps[0].rect == rect93)
    check("DrawRect color matches", rectOps[0].color == color93)

    // -------------------------------------------------------------
    // Phase 094: Text Render
    // -------------------------------------------------------------
    println("\n>>> Running Phase 094: Text Render Tests...")
    canvas92.clear()
    canvas92.drawText("Hello UI4", 15f, 25f, Color.Black, TextStyle.Title, 20f)
    val textOps = canvas92.opsOfType<RenderOp.DrawText>()
    check("Emitted DrawText operation", textOps.size == 1)
    check("DrawText content matches", textOps[0].text == "Hello UI4")
    check("DrawText coordinates match", textOps[0].x == 15f && textOps[0].y == 25f)
    check("DrawText font size matches", textOps[0].fontSize == 20f)

    // -------------------------------------------------------------
    // Phase 095: Render Tree -> Ops Display List
    // -------------------------------------------------------------
    println("\n>>> Running Phase 095: Render Tree Tests...")
    val root95 = UiRoot()
    val container95 = BoxNode().apply {
        modifier = Modifier.size(200f, 100f).background(Color(0xFFEEEEEE))
    }
    val textNode95 = TextNode("Title", TextStyle.Headline)
    container95.addChild(textNode95)
    root95.rootChild = container95
    root95.performLayout(400f, 400f)

    val canvas95 = RecordingCanvas()
    val pipeline95 = RenderPipeline()
    val res95 = pipeline95.render(root95, canvas95, force = true)

    check("Render pass succeeded", res95.rendered)
    check("Render emitted operations", res95.opCount >= 2)
    val hasBgRect = canvas95.opsOfType<RenderOp.DrawRect>().isNotEmpty()
    val hasText = canvas95.opsOfType<RenderOp.DrawText>().isNotEmpty()
    check("Display list contains background rect", hasBgRect)
    check("Display list contains text op", hasText)

    // -------------------------------------------------------------
    // Phase 096: Invalidation (Draw Only Dirty)
    // -------------------------------------------------------------
    println("\n>>> Running Phase 096: Invalidation Tests...")
    canvas95.clear()
    check("Root is clean after initial render", !root95.isDirty)
    val idleRes = pipeline95.render(root95, canvas95, force = false)
    check("Idle render skipped (opCount == 0)", !idleRes.rendered && idleRes.opCount == 0)
    check("Canvas has no operations during idle", canvas95.ops.isEmpty())

    // Mark dirty
    textNode95.markDirty()
    check("Root is dirty after child markDirty", root95.isDirty)
    val dirtyRes = pipeline95.render(root95, canvas95, force = false)
    check("Render executed after dirty invalidation", dirtyRes.rendered && dirtyRes.opCount > 0)
    check("Root is clean again after render", !root95.isDirty)

    // -------------------------------------------------------------
    // Phase 097: Clip Modifier
    // -------------------------------------------------------------
    println("\n>>> Running Phase 097: Clip Modifier Tests...")
    canvas95.clear()
    val clipNode = BoxNode().apply {
        modifier = Modifier.size(100f, 100f).clip(true)
    }
    root95.rootChild = clipNode
    root95.performLayout(400f, 400f)
    pipeline95.render(root95, canvas95, force = true)

    val clipOps = canvas95.opsOfType<RenderOp.ClipRect>()
    check("ClipRect op emitted", clipOps.isNotEmpty())
    check("Save and Restore ops emitted for clip boundary", canvas95.opsOfType<RenderOp.Save>().isNotEmpty())

    // -------------------------------------------------------------
    // Phase 098: Rounded Rectangles
    // -------------------------------------------------------------
    println("\n>>> Running Phase 098: Rounded Rect Tests...")
    canvas95.clear()
    val roundNode = BoxNode().apply {
        modifier = Modifier.size(120f, 50f).rounded(16f).background(Color.Blue)
    }
    root95.rootChild = roundNode
    root95.performLayout(400f, 400f)
    pipeline95.render(root95, canvas95, force = true)

    val roundOps = canvas95.opsOfType<RenderOp.DrawRoundRect>()
    check("DrawRoundRect op emitted", roundOps.isNotEmpty())
    check("Corner radius is 16f", roundOps[0].radius == 16f)

    // -------------------------------------------------------------
    // Phase 099: Opacity / Alpha Modifier
    // -------------------------------------------------------------
    println("\n>>> Running Phase 099: Opacity / Alpha Tests...")
    canvas95.clear()
    val alphaNode = BoxNode().apply {
        modifier = Modifier.size(100f, 100f).alpha(0.6f).background(Color.Red)
    }
    root95.rootChild = alphaNode
    root95.performLayout(400f, 400f)
    pipeline95.render(root95, canvas95, force = true)

    val alphaOps = canvas95.opsOfType<RenderOp.SetAlpha>()
    check("SetAlpha op emitted", alphaOps.isNotEmpty())
    check("Alpha value matches 0.6f", kotlin.math.abs(alphaOps[0].alpha - 0.6f) < 0.001f)

    // -------------------------------------------------------------
    // Phase 100: Render Timing & Benchmark
    // -------------------------------------------------------------
    println("\n>>> Running Phase 100: Render Benchmark Tests...")
    val benchReport = RenderBenchmark.run(root95, iterations = 50)
    check("Render benchmark completed iterations", benchReport.iterations == 50)
    check("Average render latency measured (> 0)", benchReport.avgNanos > 0)
    check("Meets 60 FPS frame budget (< 16.67ms)", benchReport.meets60FpsBudget)
    println("  Benchmark result: avg=${"%.4f".format(benchReport.avgMs)}ms, p95=${"%.4f".format(benchReport.p95Ms)}ms, opCount=${benchReport.opCount}")

    // -------------------------------------------------------------
    // Phase 101: State<T> Container
    // -------------------------------------------------------------
    println("\n>>> Running Phase 101: State<T> Tests...")
    val countState = mutableStateOf(10)
    check("Initial state value is 10", countState.value == 10)

    val stringState = state("KUI4")
    check("Initial string state is 'KUI4'", stringState.value == "KUI4")

    // -------------------------------------------------------------
    // Phase 102: State Mutation
    // -------------------------------------------------------------
    println("\n>>> Running Phase 102: State Get/Set Tests...")
    countState.value = 25
    check("Updated state value is 25", countState.value == 25)

    // -------------------------------------------------------------
    // Phase 103: Change Detection
    // -------------------------------------------------------------
    println("\n>>> Running Phase 103: Change Detection Tests...")
    var notificationCount = 0
    val sub103 = countState.subscribe { notificationCount++ }
    countState.value = 25 // Same value!
    check("Equal value does NOT trigger notification (Change Detection)", notificationCount == 0)
    countState.value = 30 // Different value!
    check("Different value triggers notification", notificationCount == 1)
    sub103.unsubscribe()

    // -------------------------------------------------------------
    // Phase 104: State Subscriptions
    // -------------------------------------------------------------
    println("\n>>> Running Phase 104: Subscriptions Tests...")
    var lastVal = 0
    val sub104 = countState.subscribe { lastVal = it }
    countState.value = 50
    check("Subscriber received new value 50", lastVal == 50)
    sub104.unsubscribe()
    countState.value = 100
    check("Unsubscribed observer does not receive updates", lastVal == 50)

    // -------------------------------------------------------------
    // Phase 105: Partial Update
    // -------------------------------------------------------------
    println("\n>>> Running Phase 105: Partial Update Tests...")
    val root105 = UiRoot()
    val col105 = ColumnNode()
    val nodeA = TextNode("A")
    val nodeB = TextNode("B")
    col105.addChild(nodeA)
    col105.addChild(nodeB)
    root105.rootChild = col105
    root105.cleanDirty()
    check("Both nodes initially clean", !nodeA.isDirty && !nodeB.isDirty)

    val dynState = mutableStateOf("Initial")
    nodeA.bindText(dynState)
    root105.cleanDirty()

    // Mutate state
    dynState.value = "Updated"
    check("Bound node A updated text", nodeA.text == "Updated")
    check("Bound node A marked dirty", nodeA.isDirty)
    check("Sibling node B remains CLEAN (Partial Update)", !nodeB.isDirty)

    // -------------------------------------------------------------
    // Phase 106: Counter Demo
    // -------------------------------------------------------------
    println("\n>>> Running Phase 106: Counter Demo Tests...")
    val counter = mutableStateOf(0)
    var counterBtnClicked = 0
    val demoApp = app {
        screen {
            column {
                text(counter)
                button("Increment") {
                    counter.value++
                    counterBtnClicked++
                }
            }
        }
    }
    val host106 = VirtualHost()
    host106.attach(demoApp)

    val colNode = (demoApp.rootChild as BoxNode).children[0] as ColumnNode
    val textCounterNode = colNode.children[0] as TextNode
    val incButton = colNode.children[1] as ButtonNode

    check("Initial counter text is '0'", textCounterNode.text == "0")
    incButton.click()
    check("Counter value incremented to 1", counter.value == 1)
    check("Bound TextNode text updated to '1'", textCounterNode.text == "1")

    // -------------------------------------------------------------
    // Phase 107: Hit Testing
    // -------------------------------------------------------------
    println("\n>>> Running Phase 107: Hit Testing Tests...")
    val root107 = UiRoot()
    val container107 = BoxNode().apply {
        modifier = Modifier.size(300f, 300f)
    }
    val childBtn = ButtonNode("Target").apply {
        modifier = Modifier.size(100f, 40f)
    }
    container107.addChild(childBtn)
    root107.rootChild = container107
    root107.performLayout(500f, 500f)

    // childBtn is centered in 500x500 viewport -> bounds approx (200, 230, 100, 40)
    val btnCenterX = childBtn.bounds.centerX
    val btnCenterY = childBtn.bounds.centerY

    val hitNode = HitTest.hitTest(root107, Point(btnCenterX, btnCenterY))
    check("HitTest found childBtn at center coordinate", hitNode === childBtn)

    val missNode = HitTest.hitTest(root107, Point(999f, 999f))
    check("HitTest returns null outside tree bounds", missNode == null)

    // -------------------------------------------------------------
    // Phase 108: Press State
    // -------------------------------------------------------------
    println("\n>>> Running Phase 108: Press State Tests...")
    val input108 = InputManager(root107)
    check("Button initially not pressed", !childBtn.isPressed)

    input108.dispatchPointer(PointerEvent(PointerEventType.Down, Point(btnCenterX, btnCenterY)))
    check("Button isPressed after Pointer Down", childBtn.isPressed)

    input108.dispatchPointer(PointerEvent(PointerEventType.Move, Point(10f, 10f)))
    check("Button isPressed is false when pointer moves outside bounds", !childBtn.isPressed)

    input108.dispatchPointer(PointerEvent(PointerEventType.Move, Point(btnCenterX, btnCenterY)))
    check("Button isPressed restored when pointer moves back inside", childBtn.isPressed)

    // -------------------------------------------------------------
    // Phase 109: Click Detection
    // -------------------------------------------------------------
    println("\n>>> Running Phase 109: Click Detection Tests...")
    var clickTriggered = false
    childBtn.onClick = { clickTriggered = true }

    // Down + Up inside bounds
    input108.dispatchPointer(PointerEvent(PointerEventType.Down, Point(btnCenterX, btnCenterY)))
    input108.dispatchPointer(PointerEvent(PointerEventType.Up, Point(btnCenterX, btnCenterY)))
    check("Click callback triggered on Down + Up inside bounds", clickTriggered)
    check("Button isPressed is false after Up", !childBtn.isPressed)

    // Down inside + Up outside should NOT click
    clickTriggered = false
    input108.dispatchPointer(PointerEvent(PointerEventType.Down, Point(btnCenterX, btnCenterY)))
    input108.dispatchPointer(PointerEvent(PointerEventType.Up, Point(10f, 10f)))
    check("Click callback NOT triggered if Up is outside bounds", !clickTriggered)

    // -------------------------------------------------------------
    // Phase 110: Button Component Integration
    // -------------------------------------------------------------
    println("\n>>> Running Phase 110: Button Component Tests...")
    var buttonEventCount = 0
    val testBtn = ButtonNode("Test") { buttonEventCount++ }
    testBtn.layout(Point(0f, 0f), Size(120f, 40f))
    check("Button is focusable by default", testBtn.isFocusable)
    testBtn.click()
    check("Button click invokes callback", buttonEventCount == 1)

    // -------------------------------------------------------------
    // Phase 111: Disabled State
    // -------------------------------------------------------------
    println("\n>>> Running Phase 111: Disabled Tests...")
    testBtn.enabled = false
    testBtn.click()
    check("Disabled button ignores click", buttonEventCount == 1)

    val input111 = InputManager(testBtn)
    val consumed = input111.dispatchPointer(PointerEvent(PointerEventType.Down, Point(10f, 10f)))
    check("Disabled node rejects pointer down event", !consumed)
    check("Disabled node is not pressed", !testBtn.isPressed)

    // -------------------------------------------------------------
    // Phase 112: Focus Management
    // -------------------------------------------------------------
    println("\n>>> Running Phase 112: Focus Management Tests...")
    val root112 = UiRoot()
    val col112 = ColumnNode()
    val btnA = ButtonNode("A")
    val btnB = ButtonNode("B")
    val btnC = ButtonNode("C").apply { enabled = false } // Disabled, not focusable
    col112.addChild(btnA)
    col112.addChild(btnB)
    col112.addChild(btnC)
    root112.rootChild = col112

    val focusManager = FocusManager(root112)
    check("Initially no focused node", focusManager.focusedNode == null)

    focusManager.requestFocus(btnA)
    check("btnA is focused", btnA.isFocused && focusManager.focusedNode === btnA)

    focusManager.focusNext()
    check("btnB is focused after focusNext()", btnB.isFocused && !btnA.isFocused)

    focusManager.focusNext()
    check("focusNext() wraps around and skips disabled btnC", btnA.isFocused && !btnB.isFocused)

    focusManager.focusPrevious()
    check("focusPrevious() wraps back to btnB", btnB.isFocused)

    focusManager.clearFocus()
    check("Focus cleared", focusManager.focusedNode == null && !btnB.isFocused)

    // -------------------------------------------------------------
    // Phase 113: Keyboard Event Model
    // -------------------------------------------------------------
    println("\n>>> Running Phase 113: Keyboard Event Model Tests...")
    val keyEvent = KeyEvent(code = KeyCode.Enter, action = KeyAction.Down, isShiftPressed = true)
    check("KeyEvent code is Enter", keyEvent.code == KeyCode.Enter)
    check("KeyEvent action is Down", keyEvent.isActionDown)
    check("KeyEvent shift modifier active", keyEvent.isShiftPressed)

    // -------------------------------------------------------------
    // Phase 114: Keyboard Activation
    // -------------------------------------------------------------
    println("\n>>> Running Phase 114: Keyboard Activation Tests...")
    var kbClickCount = 0
    btnA.onClick = { kbClickCount++ }
    focusManager.requestFocus(btnA)

    val input114 = InputManager(root112, focusManager)

    // Enter key
    input114.dispatchKey(KeyEvent(KeyCode.Enter, KeyAction.Down))
    check("btnA pressed on Enter Down", btnA.isPressed)
    input114.dispatchKey(KeyEvent(KeyCode.Enter, KeyAction.Up))
    check("btnA clicked on Enter Up", kbClickCount == 1)
    check("btnA unpressed after Enter Up", !btnA.isPressed)

    // Space key
    input114.dispatchKey(KeyEvent(KeyCode.Space, KeyAction.Down))
    input114.dispatchKey(KeyEvent(KeyCode.Space, KeyAction.Up))
    check("btnA clicked on Space Up", kbClickCount == 2)

    // Tab key navigation via dispatchKey
    input114.dispatchKey(KeyEvent(KeyCode.Tab, KeyAction.Down))
    check("Tab key shifted focus to btnB", btnB.isFocused)

    // -------------------------------------------------------------
    // Phase 115: Gesture Recognizer
    // -------------------------------------------------------------
    println("\n>>> Running Phase 115: Gesture Recognizer Tests...")
    var recognizedTap: Point? = null
    val tapRecognizer = TapGestureRecognizer(slopDistance = 15f) { pt ->
        recognizedTap = pt
    }

    check("Initial recognizer state is Possible", tapRecognizer.state == GestureState.Possible)
    tapRecognizer.processPointerEvent(PointerEvent(PointerEventType.Down, Point(50f, 50f)))
    check("State is Began after Down", tapRecognizer.state == GestureState.Began)

    tapRecognizer.processPointerEvent(PointerEvent(PointerEventType.Move, Point(52f, 53f)))
    check("State is Changed after small move within slop", tapRecognizer.state == GestureState.Changed)

    tapRecognizer.processPointerEvent(PointerEvent(PointerEventType.Up, Point(52f, 53f)))
    check("State is Recognized after Up", tapRecognizer.state == GestureState.Recognized)
    check("Tap callback received final point", recognizedTap == Point(52f, 53f))

    // Excessive drag failure test
    tapRecognizer.reset()
    tapRecognizer.processPointerEvent(PointerEvent(PointerEventType.Down, Point(50f, 50f)))
    tapRecognizer.processPointerEvent(PointerEvent(PointerEventType.Move, Point(100f, 100f))) // 70px drag > 15px
    check("Recognizer Failed after excessive displacement", tapRecognizer.state == GestureState.Failed)

    // -------------------------------------------------------------
    // Summary
    // -------------------------------------------------------------
    println("--------------------------------------------------")
    println("Summary: $passed PASSED, $failed FAILED")
    println("==================================================")
    if (failed > 0) {
        println("RESULT: FAIL")
        System.exit(1)
    } else {
        println("RESULT: PASS")
    }
}

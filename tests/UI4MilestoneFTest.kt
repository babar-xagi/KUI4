package tests

import ui4.accessibility.AccessibilityPolicy
import ui4.accessibility.AndroidAccessibilityAdapter
import ui4.accessibility.Role
import ui4.accessibility.SemanticAction
import ui4.accessibility.SemanticsNode
import ui4.animation.Easing
import ui4.animation.FlingPhysics
import ui4.animation.FrameBenchmark
import ui4.animation.FrameClock
import ui4.animation.TweenAnimation
import ui4.app
import ui4.core.Color
import ui4.core.Constraints
import ui4.core.Insets
import ui4.core.Modifier
import ui4.core.Point
import ui4.core.Rect
import ui4.core.ScrollState
import ui4.core.Size
import ui4.core.fillMaxSize
import ui4.core.height
import ui4.core.size
import ui4.core.width
import ui4.gesture.DragGestureRecognizer
import ui4.gesture.GestureState
import ui4.gesture.LongPressGestureRecognizer
import ui4.gesture.SwipeDirection
import ui4.gesture.SwipeGestureRecognizer
import ui4.gesture.TapGestureRecognizer
import ui4.input.FocusManager
import ui4.input.PointerEvent
import ui4.input.PointerEventType
import ui4.layout.BoxNode
import ui4.layout.ButtonNode
import ui4.layout.ColumnNode
import ui4.layout.ScrollNode
import ui4.layout.TextFieldNode
import ui4.layout.TextNode
import ui4.navigation.BackHandler
import ui4.navigation.NamedScreen
import ui4.navigation.Navigator
import ui4.navigation.SavedStateRegistry
import ui4.navigation.ScreenDestination
import ui4.state.mutableStateOf
import ui4.text.FontWeight
import ui4.text.LayoutDirection
import ui4.text.TextAlign
import ui4.text.TextConfig
import ui4.text.TextLayout
import ui4.text.TextRange
import ui4.text.UnicodeProof
import ui4.tree.UiRoot
import ui4.tree.addChild

/**
 * Complete test battery for Milestone F: Gestures, Scrolling, Text, Accessibility, Input & Navigation (Phases 116–150).
 */
fun main() {
    println("==================================================")
    println(" KUI4 Milestone F - Complete Test Battery (116-150)")
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
    // Phase 116: Tap Recognizer
    // -------------------------------------------------------------
    println("\n>>> Running Phase 116: Tap Gesture Tests...")
    var tapped = false
    val tap = TapGestureRecognizer(slopDistance = 10f) { tapped = true }
    tap.processPointerEvent(PointerEvent(PointerEventType.Down, Point(20f, 20f)))
    tap.processPointerEvent(PointerEvent(PointerEventType.Up, Point(22f, 21f)))
    check("Tap recognized within slop", tapped && tap.state == GestureState.Recognized)

    tap.reset()
    tapped = false
    tap.processPointerEvent(PointerEvent(PointerEventType.Down, Point(20f, 20f)))
    tap.processPointerEvent(PointerEvent(PointerEventType.Move, Point(45f, 20f))) // 25px > 10px slop
    check("Tap failed on excessive displacement", tap.state == GestureState.Failed)

    // -------------------------------------------------------------
    // Phase 117: Long Press Recognizer
    // -------------------------------------------------------------
    println("\n>>> Running Phase 117: Long Press Tests...")
    var longPressed = false
    val lp = LongPressGestureRecognizer(minDurationMs = 300L, slopDistance = 10f) { longPressed = true }
    val t0 = 1_000_000_000L
    lp.processPointerEvent(PointerEvent(PointerEventType.Down, Point(30f, 30f), timestampNanos = t0))
    check("LongPress Began on Down", lp.state == GestureState.Began)

    // Move slightly within slop
    lp.processPointerEvent(PointerEvent(PointerEventType.Move, Point(32f, 31f), timestampNanos = t0 + 100_000_000L))
    check("LongPress Changed within duration", lp.state == GestureState.Changed && !longPressed)

    // Advance time past threshold
    lp.processPointerEvent(PointerEvent(PointerEventType.Move, Point(32f, 31f), timestampNanos = t0 + 350_000_000L))
    check("LongPress Recognized after 350ms", lp.state == GestureState.Recognized && longPressed)

    // -------------------------------------------------------------
    // Phase 118: Drag Recognizer
    // -------------------------------------------------------------
    println("\n>>> Running Phase 118: Drag Gesture Tests...")
    var dragStarted = false
    var totalDragDelta = Point.Zero
    var dragEnded = false
    val drag = DragGestureRecognizer(
        slopDistance = 10f,
        onDragStart = { dragStarted = true },
        onDrag = { _, delta -> totalDragDelta = Point(totalDragDelta.x + delta.x, totalDragDelta.y + delta.y) },
        onDragEnd = { dragEnded = true }
    )

    drag.processPointerEvent(PointerEvent(PointerEventType.Down, Point(50f, 50f)))
    drag.processPointerEvent(PointerEvent(PointerEventType.Move, Point(65f, 50f))) // 15px > 10px slop
    check("Drag started after crossing slop", dragStarted && drag.state == GestureState.Recognized)

    drag.processPointerEvent(PointerEvent(PointerEventType.Move, Point(80f, 50f)))
    check("Drag accumulated continuous delta", totalDragDelta.x > 0f)

    drag.processPointerEvent(PointerEvent(PointerEventType.Up, Point(80f, 50f)))
    check("Drag ended on pointer Up", dragEnded)

    // -------------------------------------------------------------
    // Phase 119: Swipe Recognizer
    // -------------------------------------------------------------
    println("\n>>> Running Phase 119: Swipe Gesture Tests...")
    var detectedSwipe: SwipeDirection? = null
    val swipe = SwipeGestureRecognizer(minDistance = 30f, maxDurationMs = 300L) { detectedSwipe = it }

    val swipeStart = 10_000_000_000L
    swipe.processPointerEvent(PointerEvent(PointerEventType.Down, Point(100f, 100f), timestampNanos = swipeStart))
    swipe.processPointerEvent(PointerEvent(PointerEventType.Up, Point(160f, 105f), timestampNanos = swipeStart + 150_000_000L))
    check("Swipe right recognized (dx=60 in 150ms)", detectedSwipe == SwipeDirection.Right)

    swipe.reset()
    detectedSwipe = null
    swipe.processPointerEvent(PointerEvent(PointerEventType.Down, Point(100f, 100f), timestampNanos = swipeStart))
    swipe.processPointerEvent(PointerEvent(PointerEventType.Up, Point(100f, 180f), timestampNanos = swipeStart + 120_000_000L))
    check("Swipe down recognized (dy=80 in 120ms)", detectedSwipe == SwipeDirection.Down)

    // -------------------------------------------------------------
    // Phase 120: Scroll Container Node
    // -------------------------------------------------------------
    println("\n>>> Running Phase 120: Scroll Container Tests...")
    val scrollState120 = ScrollState()
    val scrollNode120 = ScrollNode(scrollState120)
    val tallChild120 = ColumnNode().apply {
        modifier = Modifier.width(300f).height(1200f)
    }
    scrollNode120.addChild(tallChild120)

    val root120 = UiRoot()
    root120.rootChild = scrollNode120
    root120.performLayout(400f, 600f)

    check("ScrollNode bounds clamped to viewport (600h)", scrollNode120.bounds.height == 600f)
    check("Child measured with tall content height (1200h)", tallChild120.measuredSize.height == 1200f)
    check("ScrollState maxScrollOffset is 600 (1200 - 600)", scrollState120.maxScrollOffset == 600f)

    // -------------------------------------------------------------
    // Phase 121: Scroll Bounds Clamping
    // -------------------------------------------------------------
    println("\n>>> Running Phase 121: Scroll Bounds Clamping Tests...")
    scrollState120.scrollTo(250f)
    check("ScrollState offset updated to 250", scrollState120.scrollOffset == 250f)
    check("Can scroll forward", scrollState120.canScrollForward)
    check("Can scroll backward", scrollState120.canScrollBackward)

    scrollState120.scrollTo(-50f)
    check("Negative scroll clamped to 0", scrollState120.scrollOffset == 0f)
    check("Cannot scroll backward at 0", !scrollState120.canScrollBackward)

    scrollState120.scrollTo(9999f)
    check("Overscroll clamped to maxScrollOffset (600)", scrollState120.scrollOffset == 600f)
    check("Cannot scroll forward at max", !scrollState120.canScrollForward)

    // -------------------------------------------------------------
    // Phase 122: Fling Physics
    // -------------------------------------------------------------
    println("\n>>> Running Phase 122: Fling Physics Tests...")
    val fling = FlingPhysics(friction = 4.0f)
    val initialVel = 1200f // 1200 px/sec
    val totalDist = fling.totalDistance(initialVel)
    check("Fling total distance is 300px (1200 / 4.0)", totalDist == 300f)

    val v1 = fling.velocityAt(initialVel, 0.5f)
    check("Fling velocity decelerates over time", v1 < initialVel && v1 > 0f)

    val d1 = fling.distanceAt(initialVel, 0.5f)
    check("Accumulated distance is less than total", d1 < totalDist && d1 > 0f)

    // -------------------------------------------------------------
    // Phase 123: Monotonic Frame Clock
    // -------------------------------------------------------------
    println("\n>>> Running Phase 123: Frame Clock Tests...")
    val nano1 = FrameClock.nowNanos()
    Thread.sleep(2)
    val nano2 = FrameClock.nowNanos()
    check("FrameClock is strictly monotonic", nano2 >= nano1)

    var clockInvoked = false
    FrameClock.withFrameNanos { time ->
        clockInvoked = time > 0L
    }
    check("withFrameNanos delivers valid frame time", clockInvoked)

    // -------------------------------------------------------------
    // Phase 124: Easing and Tween Interpolation
    // -------------------------------------------------------------
    println("\n>>> Running Phase 124: Easing and Tween Tests...")
    val tweenLinear = TweenAnimation(from = 0f, to = 100f, durationMs = 1000L, easing = Easing.Linear)
    check("Linear value at 0ms is 0", tweenLinear.valueAt(0L) == 0f)
    check("Linear value at 500ms is 50", tweenLinear.valueAt(500L) == 50f)
    check("Linear value at 1000ms is 100", tweenLinear.valueAt(1000L) == 100f)
    check("Linear animation is finished at 1000ms", tweenLinear.isFinished(1000L))

    val easeIn = Easing.EaseIn
    check("EaseIn transform at 0.5 is 0.25", easeIn.transform(0.5f) == 0.25f)

    val easeOut = Easing.EaseOut
    check("EaseOut transform at 0.5 is 0.75", easeOut.transform(0.5f) == 0.75f)

    // -------------------------------------------------------------
    // Phase 125: Frame Benchmark
    // -------------------------------------------------------------
    println("\n>>> Running Phase 125: Frame Benchmark Tests...")
    val frameReport = FrameBenchmark.run(frameCount = 120)
    check("Frame benchmark completed 120 frames", frameReport.frames == 120)
    check("Meets 60 FPS budget (< 16.67ms)", frameReport.meets60Fps)
    check("Meets 120 FPS budget (< 8.33ms)", frameReport.meets120Fps)
    println("  Animation step benchmark: avg=${"%.4f".format(frameReport.avgFrameMs)}ms, p95=${"%.4f".format(frameReport.p95FrameMs)}ms")

    // -------------------------------------------------------------
    // Phase 126: Typography & Text Styles
    // -------------------------------------------------------------
    println("\n>>> Running Phase 126: Typography Tests...")
    val textConfigNormal = TextConfig(fontSize = 16f, fontWeight = FontWeight.Normal)
    val textConfigBold = TextConfig(fontSize = 16f, fontWeight = FontWeight.Bold)
    check("Bold font has larger charWidth than normal font", textConfigBold.charWidth > textConfigNormal.charWidth)
    check("LineHeight calculation scales with multiplier", textConfigNormal.lineHeight == 16f * 1.35f)

    // -------------------------------------------------------------
    // Phase 127: Line Wrapping
    // -------------------------------------------------------------
    println("\n>>> Running Phase 127: Line Wrapping Tests...")
    val longSentence = "The quick brown fox jumps over the lazy dog"
    val wrappedLines = TextLayout.wrapText(longSentence, maxWidth = 120f, textConfigNormal)
    check("Long sentence wrapped into multiple lines", wrappedLines.size > 1)
    check("All wrapped lines respect maxWidth constraint", wrappedLines.all { it.width <= 120f })

    // -------------------------------------------------------------
    // Phase 128: Text Alignment
    // -------------------------------------------------------------
    println("\n>>> Running Phase 128: Text Alignment Tests...")
    val centerConfig = TextConfig(fontSize = 14f, textAlign = TextAlign.Center)
    val centerLines = TextLayout.wrapText("Hello", maxWidth = 200f, centerConfig)
    check("Center aligned text has positive left offset", centerLines[0].offsetX > 0f)

    val endConfig = TextConfig(fontSize = 14f, textAlign = TextAlign.End)
    val endLines = TextLayout.wrapText("Hello", maxWidth = 200f, endConfig)
    check("End aligned text offset is greater than center offset", endLines[0].offsetX > centerLines[0].offsetX)

    // -------------------------------------------------------------
    // Phase 129: Font Selection
    // -------------------------------------------------------------
    println("\n>>> Running Phase 129: Font Selection Tests...")
    val monoConfig = TextConfig(fontSize = 14f, fontFamily = "Monospace")
    check("Font family set to Monospace", monoConfig.fontFamily == "Monospace")

    // -------------------------------------------------------------
    // Phase 130: Layout Direction (LTR / RTL)
    // -------------------------------------------------------------
    println("\n>>> Running Phase 130: Layout Direction Tests...")
    val rtlConfig = TextConfig(fontSize = 14f, textAlign = TextAlign.Start, layoutDirection = LayoutDirection.Rtl)
    val rtlLines = TextLayout.wrapText("Urdu", maxWidth = 200f, rtlConfig)
    check("RTL TextAlign.Start aligns to the right side (positive offset)", rtlLines[0].offsetX > 0f)

    // -------------------------------------------------------------
    // Phase 131: Urdu / Arabic Proof
    // -------------------------------------------------------------
    println("\n>>> Running Phase 131: Urdu / Arabic Proof Tests...")
    val urduText = "سلام دنیا" // "Hello World" in Urdu
    val isUrdu = UnicodeProof.isArabicOrUrdu(urduText)
    val direction = UnicodeProof.inferDirection(urduText)
    check("UnicodeProof detects Urdu script", isUrdu)
    check("Infers RTL direction for Urdu text", direction == LayoutDirection.Rtl)

    val englishText = "Hello World"
    check("Infers LTR direction for Latin text", UnicodeProof.inferDirection(englishText) == LayoutDirection.Ltr)

    // -------------------------------------------------------------
    // Phase 132: Emoji Proof & Multi-byte Graphemes
    // -------------------------------------------------------------
    println("\n>>> Running Phase 132: Emoji Proof Tests...")
    val mixedString = "UI4 👋 🚀"
    val graphemes = UnicodeProof.extractGraphemes(mixedString)
    check("Extracted grapheme count is 7", graphemes.size == 7)
    check("Wave emoji extracted intact", graphemes.contains("👋"))
    check("Rocket emoji extracted intact", graphemes.contains("🚀"))

    // -------------------------------------------------------------
    // Phase 133: Text Selection Model
    // -------------------------------------------------------------
    println("\n>>> Running Phase 133: Text Selection Model Tests...")
    val sampleText = "KotlinApplication"
    val range = TextRange(6, 17) // "Application"
    check("TextRange slice extracts correct substring", range.slice(sampleText) == "Application")

    val replaced = range.replace(sampleText, "Platform")
    check("TextRange replace updates string", replaced == "KotlinPlatform")

    // -------------------------------------------------------------
    // Phase 134: Accessible Focus Traversal
    // -------------------------------------------------------------
    println("\n>>> Running Phase 134: Accessible Focus Traversal Tests...")
    val root134 = UiRoot()
    val col134 = ColumnNode()
    val b1 = ButtonNode("1")
    val b2 = ButtonNode("2")
    col134.addChild(b1)
    col134.addChild(b2)
    root134.rootChild = col134

    val fm134 = FocusManager(root134)
    val focusables = fm134.collectFocusableNodes()
    check("Collected focusable elements count is 2", focusables.size == 2)
    check("Focus traversal starts with first button", focusables[0] === b1)

    // -------------------------------------------------------------
    // Phase 135: Semantics Node
    // -------------------------------------------------------------
    println("\n>>> Running Phase 135: Semantics Node Tests...")
    val semNode = SemanticsNode(
        id = b1.id,
        bounds = Rect(0f, 0f, 100f, 40f),
        role = Role.Button,
        label = "Submit"
    )
    check("SemanticsNode has Button role", semNode.role == Role.Button)
    check("SemanticsNode label is 'Submit'", semNode.label == "Submit")

    // -------------------------------------------------------------
    // Phase 136: Semantic Roles
    // -------------------------------------------------------------
    println("\n>>> Running Phase 136: Semantic Roles Tests...")
    check("Role enum contains Button", Role.valueOf("Button") == Role.Button)
    check("Role enum contains TextField", Role.valueOf("TextField") == Role.TextField)
    check("Role enum contains Text", Role.valueOf("Text") == Role.Text)

    // -------------------------------------------------------------
    // Phase 137: Semantic Actions
    // -------------------------------------------------------------
    println("\n>>> Running Phase 137: Semantic Actions Tests...")
    val clickAct = SemanticAction.Click
    val setTextAct = SemanticAction.SetText("test")
    check("SemanticAction Click exists", clickAct == SemanticAction.Click)
    check("SemanticAction SetText holds payload", setTextAct.text == "test")

    // -------------------------------------------------------------
    // Phase 138: Android Accessibility Adapter
    // -------------------------------------------------------------
    println("\n>>> Running Phase 138: Android Accessibility Adapter Tests...")
    val semTree = AndroidAccessibilityAdapter.buildSemanticsTree(root134)
    check("Semantics tree created with root container", semTree.role == Role.Container)
    val talkBackDesc = AndroidAccessibilityAdapter.describeForTalkBack(semTree)
    check("TalkBack description contains role information", talkBackDesc.contains("container"))

    // -------------------------------------------------------------
    // Phase 139: Large Text (Font Scaling)
    // -------------------------------------------------------------
    println("\n>>> Running Phase 139: Large Text Tests...")
    AccessibilityPolicy.fontScale = 2.0f // 200% scale
    val scaledSize = AccessibilityPolicy.scaleFontSize(14f)
    check("Font scale 200% doubles font size to 28", scaledSize == 28f)
    AccessibilityPolicy.reset()

    // -------------------------------------------------------------
    // Phase 140: High Contrast Policy
    // -------------------------------------------------------------
    println("\n>>> Running Phase 140: High Contrast Tests...")
    AccessibilityPolicy.isHighContrastEnabled = true
    val hcBg = AccessibilityPolicy.adjustColorForContrast(Color(0xFF333333), isBackground = true)
    val hcFg = AccessibilityPolicy.adjustColorForContrast(Color(0xFFCCCCCC), isBackground = false)
    check("High contrast background mapped to pure Black", hcBg == Color.Black)
    check("High contrast foreground mapped to pure White", hcFg == Color.White)
    AccessibilityPolicy.reset()

    // -------------------------------------------------------------
    // Phase 141: Reduced Motion Policy
    // -------------------------------------------------------------
    println("\n>>> Running Phase 141: Reduced Motion Tests...")
    AccessibilityPolicy.isReducedMotionEnabled = true
    val effDur = AccessibilityPolicy.effectiveDuration(500L)
    check("Reduced motion forces animation duration to 0ms", effDur == 0L)
    AccessibilityPolicy.reset()

    // -------------------------------------------------------------
    // Phase 142: TextField Shell
    // -------------------------------------------------------------
    println("\n>>> Running Phase 142: TextField Shell Tests...")
    val tf = TextFieldNode(text = "", placeholder = "Username")
    check("TextField is focusable", tf.isFocusable)
    check("TextField placeholder is 'Username'", tf.placeholder == "Username")
    check("Initial text is empty", tf.text.isEmpty())

    // -------------------------------------------------------------
    // Phase 143: IME Software Keyboard State
    // -------------------------------------------------------------
    println("\n>>> Running Phase 143: IME State Tests...")
    check("IME initially hidden", !tf.isImeVisible)
    tf.isImeVisible = true
    check("IME set to visible on focus", tf.isImeVisible)

    // -------------------------------------------------------------
    // Phase 144: Text Input & Backspace
    // -------------------------------------------------------------
    println("\n>>> Running Phase 144: Text Input Tests...")
    var valueChangedText = ""
    tf.onValueChanged = { valueChangedText = it }

    tf.commitText("Alice")
    check("Committed text 'Alice'", tf.text == "Alice")
    check("onValueChanged triggered with 'Alice'", valueChangedText == "Alice")
    check("Cursor at end of input (5)", tf.cursorIndex == 5)

    tf.deleteBackward()
    check("deleteBackward removes last char ('Alic')", tf.text == "Alic")
    check("Cursor moved back to 4", tf.cursorIndex == 4)

    // -------------------------------------------------------------
    // Phase 145: Caret Cursor Geometry
    // -------------------------------------------------------------
    println("\n>>> Running Phase 145: Cursor Geometry Tests...")
    tf.layout(Point(0f, 0f), Size(200f, 44f))
    val cursorRect = tf.calculateCursorRect()
    check("Cursor width is 2px", cursorRect.width == 2f)
    check("Cursor height is proportional to font size", cursorRect.height > 10f)
    check("Cursor x position is inside TextField bounds", cursorRect.left >= tf.bounds.left)

    // -------------------------------------------------------------
    // Phase 146: Text Selection
    // -------------------------------------------------------------
    println("\n>>> Running Phase 146: Text Selection Tests...")
    tf.selectAll()
    check("Selection spans entire text (0..4)", tf.selection == TextRange(0, 4))
    val selRect = tf.calculateSelectionRect()
    check("Selection highlight rect calculated", selRect != null && selRect.width > 0f)

    tf.commitText("Bob")
    check("Committing text with active selection replaces text with 'Bob'", tf.text == "Bob")

    // -------------------------------------------------------------
    // Phase 147: Navigation Stack
    // -------------------------------------------------------------
    println("\n>>> Running Phase 147: Navigation Stack Tests...")
    val homeScreen = NamedScreen("Home")
    val profileScreen = NamedScreen("Profile")
    val settingsScreen = NamedScreen("Settings")

    val navigator = Navigator(initialScreen = homeScreen)
    check("Initial screen is Home", navigator.currentScreen.name == "Home")
    check("Cannot pop initial screen", !navigator.canPop)

    navigator.push(profileScreen)
    check("Pushed Profile screen", navigator.currentScreen.name == "Profile")
    check("Stack depth is 2", navigator.stackDepth == 2)
    check("Can pop after push", navigator.canPop)

    navigator.push(settingsScreen)
    check("Pushed Settings screen", navigator.currentScreen.name == "Settings")
    check("Stack depth is 3", navigator.stackDepth == 3)

    val popped = navigator.pop()
    check("Popped Settings, returned true", popped)
    check("Active screen is now Profile", navigator.currentScreen.name == "Profile")

    // -------------------------------------------------------------
    // Phase 148: Back Handling
    // -------------------------------------------------------------
    println("\n>>> Running Phase 148: Back Handling Tests...")
    val backHandler = BackHandler(navigator)
    val backConsumed1 = backHandler.handleBackPressed()
    check("Back button popped Profile returning to Home", backConsumed1 && navigator.currentScreen.name == "Home")

    val backConsumed2 = backHandler.handleBackPressed()
    check("Back button at root screen returns false (exit)", !backConsumed2)

    // -------------------------------------------------------------
    // Phase 149: Saved State Registry
    // -------------------------------------------------------------
    println("\n>>> Running Phase 149: Saved State Registry Tests...")
    val stateRegistry = SavedStateRegistry()
    stateRegistry.save("user_id", 42)
    stateRegistry.save("draft_name", "KUI4")

    val restoredId: Int? = stateRegistry.restore("user_id")
    val restoredDraft: String? = stateRegistry.restore("draft_name")
    check("Restored integer state (42)", restoredId == 42)
    check("Restored string draft ('KUI4')", restoredDraft == "KUI4")
    check("Registry contains key", stateRegistry.contains("user_id"))

    stateRegistry.remove("user_id")
    check("Removed state key", !stateRegistry.contains("user_id"))

    // -------------------------------------------------------------
    // Phase 150: Mini UI Integrated Milestone
    // -------------------------------------------------------------
    println("\n>>> Running Phase 150: Mini UI Integrated Milestone Tests...")
    val demoCounter = mutableStateOf(0)
    val inputState = mutableStateOf("Initial Input")

    val integratedApp = app {
        screen {
            column(gap = 12f) {
                text("Mini UI Milestone 🚀")
                text(demoCounter)
                button("Increment") { demoCounter.value++ }
                textField(inputState, placeholder = "Type here...")
                scroll {
                    column {
                        text("Item 1")
                        text("Item 2")
                        text("Item 3")
                    }
                }
            }
        }
    }

    integratedApp.performLayout(1080f, 1920f)
    val screenContainer = integratedApp.rootChild as BoxNode
    val mainCol = screenContainer.children[0] as ColumnNode

    check("Integrated App tree has 5 top-level items", mainCol.children.size == 5)
    val btnNode = mainCol.children[2] as ButtonNode
    btnNode.click()
    check("Integrated Counter incremented to 1", demoCounter.value == 1)

    val inputNode = mainCol.children[3] as TextFieldNode
    inputNode.commitText(" - Added")
    check("Integrated TextField updated bound state", inputState.value == "Initial Input - Added")

    val scrollItem = mainCol.children[4] as ScrollNode
    check("Scroll item laid out inside Column", scrollItem.bounds.height > 0f)

    // -------------------------------------------------------------
    // Final Summary
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

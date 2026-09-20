package tests

import ui4.app
import ui4.core.Alignment
import ui4.core.Color
import ui4.core.Constraints
import ui4.core.HorizontalAlign
import ui4.core.Insets
import ui4.core.Modifier
import ui4.core.NodeId
import ui4.core.NodeIdGenerator
import ui4.core.Point
import ui4.core.Rect
import ui4.core.Size
import ui4.core.VerticalAlign
import ui4.core.background
import ui4.core.fillMaxHeight
import ui4.core.fillMaxSize
import ui4.core.fillMaxWidth
import ui4.core.findBackground
import ui4.core.findPadding
import ui4.core.findSize
import ui4.core.findWeight
import ui4.core.height
import ui4.core.padding
import ui4.core.size
import ui4.core.weight
import ui4.core.width
import ui4.layout.BoxNode
import ui4.layout.ButtonNode
import ui4.layout.ColumnNode
import ui4.layout.LayoutStressBenchmark
import ui4.layout.RowNode
import ui4.layout.StackNode
import ui4.layout.TextNode
import ui4.layout.TextStyle
import ui4.tree.DebugTree
import ui4.tree.UiNode
import ui4.tree.UiRoot
import ui4.tree.addChild
import ui4.tree.cleanDirty
import ui4.tree.collectAll
import ui4.tree.countNodes
import ui4.tree.findNodeById
import ui4.tree.hasDirtyDescendant
import ui4.tree.markDirty
import ui4.tree.removeChild
import ui4.tree.traversePostOrder
import ui4.tree.traversePreOrder
import ui4.tree.treeDepth

/**
 * Complete Verification Test Suite for Milestone D: UI4 Core + Layout 🎨 (Phases 056–090).
 */
fun main() {
    println("==================================================")
    println(" Milestone D: UI4 Core + Layout Tests (056-090)")
    println("==================================================")

    var passed = 0
    var failed = 0

    fun check(name: String, condition: Boolean, details: String = "") {
        if (condition) {
            println("  [PASS] $name")
            passed++
        } else {
            println("  [FAIL] $name: $details")
            failed++
        }
    }

    // -------------------------------------------------------------
    // Phase 056: UI4 Module Bootstrap
    // -------------------------------------------------------------
    check("Phase 056: UI4 module classes loaded successfully", true)

    // -------------------------------------------------------------
    // Phase 057: NodeId
    // -------------------------------------------------------------
    NodeIdGenerator.reset(100L)
    val id1 = NodeIdGenerator.next()
    val id2 = NodeIdGenerator.next()
    check("Phase 057: NodeId generation is sequential", id1.value == 100L && id2.value == 101L)
    check("Phase 057: NodeId formatting is '#100'", id1.toString() == "#100")
    check("Phase 057: NodeId comparable works", id1 < id2)

    // -------------------------------------------------------------
    // Phase 058: Geometry (Point, Size, Insets, Rect)
    // -------------------------------------------------------------
    val p1 = Point(10f, 20f)
    val p2 = Point(5f, 15f)
    check("Phase 058: Point addition and subtraction", (p1 + p2) == Point(15f, 35f) && (p1 - p2) == Point(5f, 5f))

    val size = Size(100f, 50f)
    check("Phase 058: Size area calculation", size.area == 5000f)

    val insets = Insets(left = 10f, top = 20f, right = 15f, bottom = 25f)
    check("Phase 058: Insets horizontal and vertical sum", insets.horizontal == 25f && insets.vertical == 45f)

    val rect = Rect(left = 10f, top = 20f, width = 100f, height = 80f)
    check("Phase 058: Rect right and bottom bounds", rect.right == 110f && rect.bottom == 100f)
    check("Phase 058: Rect contains point inside", rect.contains(Point(50f, 50f)))
    check("Phase 058: Rect does not contain point outside", !rect.contains(Point(5f, 5f)))

    val deflated = rect.deflate(Insets.all(10f))
    check("Phase 058: Rect deflate shrinks bounds", deflated == Rect(20f, 30f, 80f, 60f))

    // -------------------------------------------------------------
    // Phase 059: Color (RGBA, Hex Parsing, Blending)
    // -------------------------------------------------------------
    val redColor = Color.fromRgb(255, 0, 0)
    check("Phase 059: Red color ARGB value", redColor.redInt == 255 && redColor.greenInt == 0 && redColor.blueInt == 0)
    check("Phase 059: Red hex string formatting", redColor.toHexString() == "#FFFF0000")

    val hexColor6 = Color.parseHex("#00FF00")
    check("Phase 059: Parses #RRGGBB format", hexColor6.greenInt == 255 && hexColor6.alphaInt == 255)

    val hexColor8 = Color.parseHex("#800000FF")
    check("Phase 059: Parses #AARRGGBB format", hexColor8.alphaInt == 128 && hexColor8.blueInt == 255)

    val hexColor3 = Color.parseHex("#F00")
    check("Phase 059: Parses #RGB shorthand", hexColor3.redInt == 255 && hexColor3.greenInt == 0)

    val blended = Color.lerp(Color.Black, Color.White, 0.5f)
    check("Phase 059: Color lerp produces mid-gray", blended.redInt in 127..128)

    // -------------------------------------------------------------
    // Phase 060: UiNode & Bounds
    // -------------------------------------------------------------
    val node = UiNode(tag = "CustomNode")
    check("Phase 060: UiNode default dirty is true", node.dirty)
    check("Phase 060: UiNode tag is CustomNode", node.tag == "CustomNode")

    // -------------------------------------------------------------
    // Phase 061: Children Tree Invariants & Cycle Detection
    // -------------------------------------------------------------
    val parentNode = UiNode(tag = "Parent")
    val childNode1 = UiNode(tag = "Child1")
    val childNode2 = UiNode(tag = "Child2")

    parentNode.addChild(childNode1)
    parentNode.addChild(childNode2)
    check("Phase 061: Parent has 2 children", parentNode.children.size == 2)
    check("Phase 061: Child has parent reference", childNode1.parent === parentNode)

    var cycleCaught = false
    try {
        childNode1.addChild(parentNode)
    } catch (e: IllegalArgumentException) {
        cycleCaught = true
    }
    check("Phase 061: Cycle detection prevents ancestor reparenting", cycleCaught)

    parentNode.removeChild(childNode1)
    check("Phase 061: removeChild detaches parent pointer", childNode1.parent == null && parentNode.children.size == 1)

    // -------------------------------------------------------------
    // Phase 062: Single Root Invariant
    // -------------------------------------------------------------
    val uiRoot = UiRoot()
    val rootChild1 = BoxNode()
    val rootChild2 = BoxNode()
    uiRoot.rootChild = rootChild1
    check("Phase 062: Single root child attached", uiRoot.rootChild === rootChild1)
    uiRoot.rootChild = rootChild2
    check("Phase 062: Setting root child replaces previous child", uiRoot.rootChild === rootChild2 && uiRoot.children.size == 1)

    // -------------------------------------------------------------
    // Phase 063: Traversal (DFS Pre-order, Post-order, Search)
    // -------------------------------------------------------------
    val treeRoot = UiNode(tag = "A")
    val childB = UiNode(tag = "B")
    val childC = UiNode(tag = "C")
    val grandChildD = UiNode(tag = "D")
    treeRoot.addChild(childB)
    treeRoot.addChild(childC)
    childB.addChild(grandChildD)

    val preOrderTags = mutableListOf<String>()
    treeRoot.traversePreOrder { preOrderTags.add(it.tag) }
    check("Phase 063: DFS Pre-order traversal order is [A, B, D, C]", preOrderTags == listOf("A", "B", "D", "C"))

    val postOrderTags = mutableListOf<String>()
    treeRoot.traversePostOrder { postOrderTags.add(it.tag) }
    check("Phase 063: DFS Post-order traversal order is [D, B, C, A]", postOrderTags == listOf("D", "B", "C", "A"))

    check("Phase 063: findNodeById finds grandchild", treeRoot.findNodeById(grandChildD.id) === grandChildD)
    check("Phase 063: Tree depth is 3", treeRoot.treeDepth() == 3)
    check("Phase 063: Total node count is 4", treeRoot.countNodes() == 4)

    // -------------------------------------------------------------
    // Phase 064: Dirty Flag Tracking & Propagation
    // -------------------------------------------------------------
    treeRoot.cleanDirty()
    check("Phase 064: cleanDirty clears all dirty flags", !treeRoot.hasDirtyDescendant())
    grandChildD.markDirty()
    check("Phase 064: markDirty on grandchild marks tree root dirty", treeRoot.dirty && treeRoot.hasDirtyDescendant())

    // -------------------------------------------------------------
    // Phase 065: Debug Tree Pretty Printing
    // -------------------------------------------------------------
    val debugDump = DebugTree.dumpToString(treeRoot)
    check("Phase 065: Debug tree snapshot contains root and descendants",
        debugDump.contains("A#") && debugDump.contains("B#") && debugDump.contains("D#") && debugDump.contains("C#"),
        "Dump:\n$debugDump"
    )

    // -------------------------------------------------------------
    // Phase 066: Constraints Clamping
    // -------------------------------------------------------------
    val constraints = Constraints(minWidth = 50f, maxWidth = 200f, minHeight = 40f, maxHeight = 100f)
    check("Phase 066: Constraints clamps undersized dimensions", constraints.clamp(Size(10f, 10f)) == Size(50f, 40f))
    check("Phase 066: Constraints clamps oversized dimensions", constraints.clamp(Size(500f, 500f)) == Size(200f, 100f))
    check("Phase 066: Tight constraints detection", Constraints.tight(Size(100f, 100f)).isTight)

    // -------------------------------------------------------------
    // Phase 067 & Phase 068: Measure Protocol & Final Placement
    // -------------------------------------------------------------
    val testBox = BoxNode(alignment = Alignment.Center)
    val fixedChild = UiNode(tag = "Child").apply {
        modifier = Modifier.size(60f, 40f)
    }
    testBox.addChild(fixedChild)
    val measuredBox = testBox.measure(Constraints.fixed(200f, 200f))
    check("Phase 067: Box measures to constraint", measuredBox == Size(200f, 200f))
    testBox.layout(Point.Zero, Size(200f, 200f))
    check("Phase 068: Child centered placement at (70, 80)",
        fixedChild.bounds == Rect(70f, 80f, 60f, 40f),
        "Actual bounds: ${fixedChild.bounds}"
    )

    // -------------------------------------------------------------
    // Phase 069 & Phase 070: Box & Padding Insets
    // -------------------------------------------------------------
    val paddedBox = BoxNode(alignment = Alignment.TopStart).apply {
        modifier = Modifier.padding(10f)
    }
    val innerChild = UiNode(tag = "Inner").apply {
        modifier = Modifier.size(50f, 50f)
    }
    paddedBox.addChild(innerChild)
    paddedBox.measure(Constraints.fixed(100f, 100f))
    paddedBox.layout(Point.Zero, Size(100f, 100f))
    check("Phase 069 & 070: Padding offsets child by (10, 10)",
        innerChild.bounds.origin == Point(10f, 10f)
    )

    // -------------------------------------------------------------
    // Phase 071: Alignment (Start, Center, End)
    // -------------------------------------------------------------
    val endAlignedBox = BoxNode(alignment = Alignment.BottomEnd)
    val endChild = UiNode(tag = "EndChild").apply { modifier = Modifier.size(20f, 20f) }
    endAlignedBox.addChild(endChild)
    endAlignedBox.measure(Constraints.fixed(100f, 100f))
    endAlignedBox.layout(Point.Zero, Size(100f, 100f))
    check("Phase 071: BottomEnd alignment places child at (80, 80)",
        endChild.bounds == Rect(80f, 80f, 20f, 20f)
    )

    // -------------------------------------------------------------
    // Phase 072 & Phase 074: Column Layout & Gap Spacing
    // -------------------------------------------------------------
    val col = ColumnNode(gap = 10f, horizontalAlignment = HorizontalAlign.Center)
    val colItem1 = UiNode(tag = "Item1").apply { modifier = Modifier.size(50f, 30f) }
    val colItem2 = UiNode(tag = "Item2").apply { modifier = Modifier.size(80f, 40f) }
    col.addChild(colItem1)
    col.addChild(colItem2)

    col.measure(Constraints(0f, 200f, 0f, 500f))
    col.layout(Point.Zero, Size(100f, 80f))
    check("Phase 072 & 074: Column item 1 placed at y=0", colItem1.bounds.top == 0f)
    check("Phase 072 & 074: Column item 2 placed at y = 30 + 10 = 40", colItem2.bounds.top == 40f)
    check("Phase 072: Item 1 centered horizontally (100 - 50) / 2 = 25", colItem1.bounds.left == 25f)
    check("Phase 072: Item 2 centered horizontally (100 - 80) / 2 = 10", colItem2.bounds.left == 10f)

    // -------------------------------------------------------------
    // Phase 073 & Phase 074: Row Layout & Gap Spacing
    // -------------------------------------------------------------
    val row = RowNode(gap = 15f, verticalAlignment = VerticalAlign.Center)
    val rowItem1 = UiNode(tag = "RowItem1").apply { modifier = Modifier.size(40f, 30f) }
    val rowItem2 = UiNode(tag = "RowItem2").apply { modifier = Modifier.size(60f, 50f) }
    row.addChild(rowItem1)
    row.addChild(rowItem2)

    row.measure(Constraints(0f, 500f, 0f, 100f))
    row.layout(Point.Zero, Size(200f, 100f))
    check("Phase 073 & 074: Row item 1 placed at x=0", rowItem1.bounds.left == 0f)
    check("Phase 073 & 074: Row item 2 placed at x = 40 + 15 = 55", rowItem2.bounds.left == 55f)
    check("Phase 073: Row item 1 vertically centered (100 - 30) / 2 = 35", rowItem1.bounds.top == 35f)

    // -------------------------------------------------------------
    // Phase 075: Stack Overlay Layout & Z-Ordering
    // -------------------------------------------------------------
    val stack = StackNode(alignment = Alignment.TopStart)
    val backgroundLayer = UiNode(tag = "Bg").apply { modifier = Modifier.size(200f, 200f) }
    val foregroundLayer = UiNode(tag = "Fg").apply { modifier = Modifier.size(50f, 50f) }
    stack.addChild(backgroundLayer)
    stack.addChild(foregroundLayer)

    stack.measure(Constraints.fixed(200f, 200f))
    stack.layout(Point.Zero, Size(200f, 200f))
    check("Phase 075: Stack children both placed at origin (0, 0)",
        backgroundLayer.bounds.origin == Point.Zero && foregroundLayer.bounds.origin == Point.Zero
    )
    check("Phase 075: Z-ordering preserves list ordering (Bg then Fg)",
        stack.children[0] === backgroundLayer && stack.children[1] === foregroundLayer
    )

    // -------------------------------------------------------------
    // Phase 076 & Phase 077: Fill Width & Fill Height
    // -------------------------------------------------------------
    val fillBox = BoxNode().apply {
        modifier = Modifier.fillMaxWidth(0.8f).then(Modifier.fillMaxHeight(0.5f))
    }
    val fillSize = fillBox.measure(Constraints(0f, 400f, 0f, 600f))
    check("Phase 076: Fill width 80% of 400 = 320", fillSize.width == 320f)
    check("Phase 077: Fill height 50% of 600 = 300", fillSize.height == 300f)

    // -------------------------------------------------------------
    // Phase 078: Flex Weights (Free Space Distribution)
    // -------------------------------------------------------------
    val weightedCol = ColumnNode(gap = 0f)
    val fixedPart = UiNode().apply { modifier = Modifier.height(100f) }
    val flex1 = UiNode().apply { modifier = Modifier.weight(1f) }
    val flex2 = UiNode().apply { modifier = Modifier.weight(2f) }

    weightedCol.addChild(fixedPart)
    weightedCol.addChild(flex1)
    weightedCol.addChild(flex2)

    // Total available height = 400. Fixed uses 100. Remaining free space = 300.
    // flex1 gets 1/3 = 100. flex2 gets 2/3 = 200.
    weightedCol.measure(Constraints.fixed(200f, 400f))
    check("Phase 078: Weight 1f receives 100px", flex1.measuredSize.height == 100f)
    check("Phase 078: Weight 2f receives 200px", flex2.measuredSize.height == 200f)

    // -------------------------------------------------------------
    // Phase 079: Layout Stress Benchmark (1,000 Nodes)
    // -------------------------------------------------------------
    val stressStats = LayoutStressBenchmark.run(1000)
    check("Phase 079: Stress benchmark generated >= 1000 nodes", stressStats.nodeCount >= 1000)
    check("Phase 079: Total layout duration < 20ms",
        stressStats.totalDurationMs < 20.0,
        "Total: ${stressStats.totalDurationMs}ms (Measure: ${stressStats.measureDurationMs}ms, Layout: ${stressStats.layoutDurationMs}ms)"
    )

    // -------------------------------------------------------------
    // Phase 080 - 085: Declarative DSL (app, screen, column, row, box, text)
    // -------------------------------------------------------------
    val dslRoot = app {
        screen {
            column(gap = 12f) {
                text("Welcome", style = TextStyle.Headline)
                row(gap = 6f) {
                    button("OK")
                    button("Cancel")
                }
            }
        }
    }

    check("Phase 080: Tree built from DSL successfully", dslRoot.rootChild != null)
    check("Phase 081: screen {} created Screen container", dslRoot.rootChild?.tag == "Screen")
    val screenChild = dslRoot.rootChild?.children?.firstOrNull() as? ColumnNode
    check("Phase 082: column {} created ColumnNode with gap=12", screenChild != null && screenChild.gap == 12f)

    val textChild = screenChild?.children?.firstOrNull() as? TextNode
    check("Phase 085: text() created TextNode with value 'Welcome'", textChild?.text == "Welcome")
    check("Phase 085: TextNode has Headline style", textChild?.style == TextStyle.Headline)

    val rowChild = screenChild?.children?.getOrNull(1) as? RowNode
    check("Phase 083: row {} created RowNode with gap=6", rowChild != null && rowChild.gap == 6f)
    check("Phase 085: row contains 2 ButtonNodes", rowChild != null && rowChild.children.size == 2 && rowChild.children.all { it is ButtonNode })

    // -------------------------------------------------------------
    // Phase 086: Modifier Prototype & Order Preservation
    // -------------------------------------------------------------
    val chain = Modifier.padding(16f).background(Color.Red).weight(2f)
    check("Phase 086: Modifier chain preserves elements", chain.findPadding() == Insets.all(16f))
    check("Phase 088: Modifier preserves background color metadata", chain.findBackground() == Color.Red)
    check("Phase 086: Modifier preserves weight metadata", chain.findWeight() == 2f)

    // -------------------------------------------------------------
    // Phase 087 - 089: Padding, Background, Size Modifiers
    // -------------------------------------------------------------
    val sizeChain = Modifier.width(120f).height(60f)
    check("Phase 089: Size modifier captures width and height",
        sizeChain.findSize()?.width == 120f && sizeChain.findSize()?.height == 60f
    )

    // -------------------------------------------------------------
    // Phase 090: DSL Usability & Hello Application Verification
    // -------------------------------------------------------------
    val helloAppRoot = app {
        screen {
            center {
                text("Hello, KUI4! 👋")
            }
        }
    }
    helloAppRoot.performLayout(1080f, 2400f)
    check("Phase 090: Hello application builds and layouts cleanly",
        helloAppRoot.countNodes() == 4 && helloAppRoot.bounds == Rect(0f, 0f, 1080f, 2400f)
    )

    println("==================================================")
    println(" Milestone D Results: $passed Passed, $failed Failed")
    println("==================================================")

    if (failed > 0) {
        System.exit(1)
    }
}

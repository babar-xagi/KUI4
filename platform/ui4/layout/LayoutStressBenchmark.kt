package ui4.layout

import ui4.core.Constraints
import ui4.core.Point
import ui4.core.Size
import ui4.core.padding
import ui4.core.weight
import ui4.tree.UiNode
import ui4.tree.UiRoot
import ui4.tree.addChild
import ui4.tree.countNodes

/**
 * Layout benchmark results for performance stress testing (Phase 079).
 */
data class LayoutStressStats(
    val nodeCount: Int,
    val measureDurationMs: Double,
    val layoutDurationMs: Double,
    val totalDurationMs: Double
)

/**
 * Generates and benchmarks massive UI trees to guarantee fast 60fps/120fps layout performance (Phase 079).
 */
object LayoutStressBenchmark {

    fun run(targetNodeCount: Int = 1000, viewportWidth: Float = 1080f, viewportHeight: Float = 2400f): LayoutStressStats {
        val root = UiRoot()
        val mainColumn = ColumnNode(gap = 8f)
        root.rootChild = mainColumn

        var currentNodes = 2 // root + mainColumn
        var rowIdx = 0

        while (currentNodes < targetNodeCount) {
            val row = RowNode(gap = 4f)
            mainColumn.addChild(row)
            currentNodes++

            // Add 4 children to each row
            for (i in 0 until 4) {
                if (currentNodes >= targetNodeCount) break
                val box = BoxNode()
                val text = TextNode("Item #${rowIdx}_$i")
                box.addChild(text)
                row.addChild(box)
                currentNodes += 2
            }
            rowIdx++
        }

        val constraints = Constraints.fixed(viewportWidth, viewportHeight)

        // 1. Measure pass
        val measureStart = System.nanoTime()
        root.measure(constraints)
        val measureMs = (System.nanoTime() - measureStart) / 1_000_000.0

        // 2. Layout pass
        val layoutStart = System.nanoTime()
        root.layout(Point.Zero, Size(viewportWidth, viewportHeight))
        val layoutMs = (System.nanoTime() - layoutStart) / 1_000_000.0

        return LayoutStressStats(
            nodeCount = root.countNodes(),
            measureDurationMs = measureMs,
            layoutDurationMs = layoutMs,
            totalDurationMs = measureMs + layoutMs
        )
    }
}

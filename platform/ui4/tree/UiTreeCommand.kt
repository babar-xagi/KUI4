package ui4.tree

import ui4.app
import ui4.layout.TextStyle

/**
 * CLI command `kui ui-tree` that prints the formatted UI tree and bounds (Phase 065).
 */
object UiTreeCommand {

    fun execute(args: List<String> = emptyList(), flags: Map<String, String> = emptyMap()): Int {
        println("==================================================")
        println(" 🌳  UI4 Runtime Component Tree (Debug View)")
        println("==================================================")

        // Build sample application tree
        val root = app {
            screen {
                column(gap = 16f) {
                    text("Hello, KUI4! 👋", style = TextStyle.Headline)
                    text("Pure Kotlin Declarative UI Platform", style = TextStyle.Body)
                    row(gap = 8f) {
                        button("Get Started")
                        button("Documentation")
                    }
                }
            }
        }

        val viewportW = flags["width"]?.toFloatOrNull() ?: 1080f
        val viewportH = flags["height"]?.toFloatOrNull() ?: 2400f

        root.performLayout(viewportW, viewportH)

        println(DebugTree.dumpToString(root))
        println("--------------------------------------------------")
        println("Total Nodes: ${root.countNodes()} | Tree Depth: ${root.treeDepth()}")
        println("Viewport: ${viewportW.toInt()}x${viewportH.toInt()} px")
        return 0
    }
}

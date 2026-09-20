package ui4.tree

import java.util.Locale

/**
 * Visual tree printer for diagnostics and snapshot inspection (Phase 065).
 */
object DebugTree {

    fun dumpToString(root: UiNode): String {
        val sb = StringBuilder()
        dumpNode(root, sb, "", true)
        return sb.toString().trimEnd()
    }

    private fun dumpNode(
        node: UiNode,
        sb: StringBuilder,
        prefix: String,
        isLast: Boolean
    ) {
        val marker = if (isLast) "\\-- " else "+-- "
        val boundsStr = "[%.1f, %.1f, %.1f, %.1f]".format(
            Locale.US,
            node.bounds.left,
            node.bounds.top,
            node.bounds.width,
            node.bounds.height
        )

        val extraInfo = formatExtraInfo(node)
        val dirtyStr = if (node.dirty) " dirty=true" else ""

        sb.append(prefix)
            .append(marker)
            .append(node.tag)
            .append(node.id.toString())
            .append(" ")
            .append(boundsStr)
            .append(extraInfo)
            .append(dirtyStr)
            .append("\n")

        val childPrefix = prefix + if (isLast) "    " else "|   "
        val count = node.children.size
        for (i in 0 until count) {
            dumpNode(node.children[i], sb, childPrefix, i == count - 1)
        }
    }

    private fun formatExtraInfo(node: UiNode): String {
        val props = mutableListOf<String>()

        val modStr = node.modifier.toString()
        if (modStr != "Modifier") {
            props.add("modifier=$modStr")
        }

        return if (props.isNotEmpty()) " " + props.joinToString(" ") else ""
    }
}

package ui4.accessibility

import ui4.core.Color
import ui4.core.NodeId
import ui4.core.NodeIdGenerator
import ui4.core.Rect
import ui4.tree.UiNode
import ui4.tree.traversePreOrder

/**
 * Semantic accessibility roles (Phase 136).
 */
enum class Role {
    Button,
    Text,
    Image,
    Checkbox,
    Switch,
    TextField,
    Header,
    Container
}

/**
 * Standard semantic accessibility actions (Phase 137).
 */
sealed class SemanticAction {
    object Click : SemanticAction()
    object Focus : SemanticAction()
    object ScrollForward : SemanticAction()
    object ScrollBackward : SemanticAction()
    data class SetText(val text: String) : SemanticAction()
}

/**
 * Node in the accessibility semantics tree consumed by assistive tech like TalkBack (Phase 135).
 */
data class SemanticsNode(
    val id: NodeId,
    val bounds: Rect,
    val role: Role,
    val label: String,
    val hint: String? = null,
    val value: String? = null,
    val isEnabled: Boolean = true,
    val isFocused: Boolean = false,
    val actions: List<SemanticAction> = listOf(SemanticAction.Click),
    val children: MutableList<SemanticsNode> = mutableListOf()
) {
    override fun toString(): String =
        "SemanticsNode(id=$id, role=$role, label=\"$label\", bounds=$bounds)"
}

/**
 * Global accessibility configuration and policies (Phases 139, 140, 141).
 */
object AccessibilityPolicy {

    /**
     * User system font scaling factor (1.0 = normal, 2.0 = 200% large text) (Phase 139).
     */
    var fontScale: Float = 1.0f

    /**
     * High-contrast theme mode (Phase 140).
     */
    var isHighContrastEnabled: Boolean = false

    /**
     * System reduced motion preference (Phase 141).
     */
    var isReducedMotionEnabled: Boolean = false

    /**
     * Applies accessibility font scaling to a base size.
     */
    fun scaleFontSize(baseSize: Float): Float =
        baseSize * fontScale

    /**
     * Computes high-contrast color replacement when enabled (Phase 140).
     */
    fun adjustColorForContrast(color: Color, isBackground: Boolean): Color {
        if (!isHighContrastEnabled) return color
        // High contrast forces maximum foreground / background separation
        return if (isBackground) {
            Color.Black
        } else {
            Color.White
        }
    }

    /**
     * Adjusts animation duration respecting reduced motion policy (Phase 141).
     */
    fun effectiveDuration(normalDurationMs: Long): Long =
        if (isReducedMotionEnabled) 0L else normalDurationMs

    fun reset() {
        fontScale = 1.0f
        isHighContrastEnabled = false
        isReducedMotionEnabled = false
    }
}

/**
 * Adapts UI4 hierarchy to Android AccessibilityNodeInfo contract (Phase 138).
 */
object AndroidAccessibilityAdapter {

    /**
     * Extracts a SemanticsNode tree from a UI4 node hierarchy.
     */
    fun buildSemanticsTree(root: UiNode): SemanticsNode {
        val rootSemantics = createSemantics(root)
        populateChildren(root, rootSemantics)
        return rootSemantics
    }

    private fun populateChildren(uiNode: UiNode, parentSemantics: SemanticsNode) {
        for (child in uiNode.children) {
            val childSemantics = createSemantics(child)
            parentSemantics.children.add(childSemantics)
            populateChildren(child, childSemantics)
        }
    }

    private fun createSemantics(node: UiNode): SemanticsNode {
        val role = when (node.tag) {
            "Button" -> Role.Button
            "Text" -> Role.Text
            "TextField" -> Role.TextField
            else -> Role.Container
        }

        val label = when (node) {
            is ui4.layout.ButtonNode -> node.label
            is ui4.layout.TextNode -> node.text
            else -> node.tag
        }

        val actions = mutableListOf<SemanticAction>()
        if (node.enabled) {
            actions.add(SemanticAction.Click)
            if (node.isFocusable) {
                actions.add(SemanticAction.Focus)
            }
        }

        return SemanticsNode(
            id = node.id,
            bounds = node.bounds,
            role = role,
            label = label,
            isEnabled = node.enabled,
            isFocused = node.isFocused,
            actions = actions
        )
    }

    /**
     * Simulates TalkBack node description generation.
     */
    fun describeForTalkBack(node: SemanticsNode): String {
        val stateDesc = if (!node.isEnabled) ", disabled" else ""
        val focusDesc = if (node.isFocused) ", focused" else ""
        return "${node.label}, ${node.role.name.lowercase()}$stateDesc$focusDesc"
    }
}

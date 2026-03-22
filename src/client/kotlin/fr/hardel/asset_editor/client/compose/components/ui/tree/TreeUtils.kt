package fr.hardel.asset_editor.client.compose.components.ui.tree

object TreeUtils {

    fun hasActiveDescendant(node: TreeNodeModel, activeId: String?): Boolean {
        if (activeId.isNullOrBlank()) return false
        if (activeId == node.elementId) return true
        return node.children.values.any { hasActiveDescendant(it, activeId) }
    }

    fun recalculateCount(node: TreeNodeModel): Int {
        if (!node.elementId.isNullOrBlank()) return 1
        val total = node.children.values.sumOf { recalculateCount(it) }
        node.count = total
        return total
    }
}

package fr.hardel.asset_editor.client.compose.components.ui.tree

object TreeUtils {

    fun hasActiveDescendant(node: TreeNodeModel, activeId: String?): Boolean {
        if (activeId.isNullOrBlank()) return false
        if (activeId == node.elementId) return true
        for (child in node.children.values) {
            if (hasActiveDescendant(child, activeId)) return true
        }
        return false
    }

    fun recalculateCount(node: TreeNodeModel): Int {
        if (!node.elementId.isNullOrBlank()) return 1
        var value = 0
        for (child in node.children.values) {
            value += recalculateCount(child)
        }
        node.count = value
        return value
    }
}
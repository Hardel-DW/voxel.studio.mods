package fr.hardel.asset_editor.client.compose.components.page.loot_table

import fr.hardel.asset_editor.client.compose.components.ui.tree.TreeNodeModel
import fr.hardel.asset_editor.client.compose.components.ui.tree.TreeUtils
import fr.hardel.asset_editor.client.compose.lib.data.StudioElementId

object LootTableTreeBuilder {

    fun build(elementIds: List<String>): TreeNodeModel {
        val root = TreeNodeModel()
        root.count = elementIds.size

        elementIds.forEach { elementId ->
            val parsed = StudioElementId.parse(elementId) ?: return@forEach
            val identifier = parsed.identifier
            val parts = identifier.path.split("/")

            var current = ensureFolder(root, identifier.namespace)
            for (index in 0 until parts.size - 1) {
                current = ensureFolder(current, parts[index])
            }

            val leaf = TreeNodeModel()
            leaf.count = 1
            leaf.elementId = identifier.toString()
            current.children[parts.lastOrNull() ?: identifier.path] = leaf
        }

        TreeUtils.recalculateCount(root)
        return root
    }

    private fun ensureFolder(parent: TreeNodeModel, name: String): TreeNodeModel =
        parent.children.getOrPut(name) {
            TreeNodeModel().also { it.folder = true }
        }
}

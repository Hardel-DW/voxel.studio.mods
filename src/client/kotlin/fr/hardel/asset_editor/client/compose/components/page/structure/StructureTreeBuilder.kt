package fr.hardel.asset_editor.client.compose.components.page.structure

import fr.hardel.asset_editor.client.compose.components.ui.tree.TreeNodeModel
import fr.hardel.asset_editor.client.compose.components.ui.tree.TreeUtils
import net.minecraft.resources.Identifier

object StructureTreeBuilder {
    fun <T> build(
        entries: List<T>,
        idOf: (T) -> Identifier,
        iconResolver: (Identifier) -> Identifier? = { null }
    ): TreeNodeModel {
        val root = TreeNodeModel()
        root.count = entries.size

        for (entry in entries) {
            val identifier = idOf(entry)
            val parts = identifier.path.split("/")
            val leafName = parts.lastOrNull() ?: identifier.path

            var current = ensureFolder(root, identifier.namespace)
            for (index in 0 until parts.size - 1) {
                current = ensureFolder(current, parts[index])
            }

            val leaf = TreeNodeModel()
            leaf.count = 1
            leaf.elementId = identifier.toString()
            leaf.label = leafName
            iconResolver(identifier)?.let { leaf.icon = it }
            current.children[leafName] = leaf
        }

        TreeUtils.recalculateCount(root)
        return root
    }

    private fun ensureFolder(parent: TreeNodeModel, name: String): TreeNodeModel =
        parent.children.getOrPut(name) {
            TreeNodeModel().also {
                it.folder = true
                it.label = name
            }
        }
}

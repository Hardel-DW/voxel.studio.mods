package fr.hardel.asset_editor.client.compose.components.page.structure

import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.components.ui.tree.TreeNodeModel
import fr.hardel.asset_editor.client.compose.components.ui.tree.TreeUtils
import fr.hardel.asset_editor.client.compose.lib.assets.StudioAssetCache
import fr.hardel.asset_editor.network.structure.StructureWorldgenSnapshot
import net.minecraft.resources.Identifier

private val JIGSAW_FALLBACK = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/jigsaw.svg")

object StructureWorldgenTreeBuilder {
    fun build(entries: List<StructureWorldgenSnapshot>, assetCache: StudioAssetCache): TreeNodeModel {
        val root = TreeNodeModel()
        root.count = entries.size

        for (entry in entries) {
            val identifier = entry.id()
            val parts = identifier.path.split("/")

            var current = ensureFolder(root, identifier.namespace)
            for (index in 0 until parts.size - 1) {
                current = ensureFolder(current, parts[index])
            }

            val leaf = TreeNodeModel()
            leaf.count = 1
            leaf.elementId = identifier.toString()
            leaf.label = parts.lastOrNull() ?: identifier.path
            leaf.icon = resolveIcon(identifier, assetCache)
            current.children[parts.lastOrNull() ?: identifier.path] = leaf
        }

        TreeUtils.recalculateCount(root)
        return root
    }

    private fun resolveIcon(id: Identifier, assetCache: StudioAssetCache): Identifier {
        val custom = Identifier.fromNamespaceAndPath(id.namespace, "icons/structure/${id.path}.svg")
        return if (assetCache.svg(custom) != null) custom else JIGSAW_FALLBACK
    }

    private fun ensureFolder(parent: TreeNodeModel, name: String): TreeNodeModel =
        parent.children.getOrPut(name) {
            TreeNodeModel().also {
                it.folder = true
                it.label = name
            }
        }
}

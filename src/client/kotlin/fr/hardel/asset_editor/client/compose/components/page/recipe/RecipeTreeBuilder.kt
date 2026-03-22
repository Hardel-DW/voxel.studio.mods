package fr.hardel.asset_editor.client.compose.components.page.recipe

import fr.hardel.asset_editor.client.compose.components.ui.tree.TreeNodeModel
import fr.hardel.asset_editor.client.compose.lib.data.RecipeTreeData
import net.minecraft.resources.Identifier

object RecipeTreeBuilder {

    fun build(elements: List<RecipeEntry>): TreeNodeModel {
        val root = TreeNodeModel()
        root.count = elements.size

        RecipeTreeData.RECIPE_BLOCKS.forEach { block ->
            if (block.special) {
                return@forEach
            }

            val typeStrings = block.recipeTypes.map(Identifier::toString)
            val matching = elements.filter { element -> typeStrings.contains(element.type) }
            val blockNode = TreeNodeModel()
            blockNode.count = matching.size

            if (block.recipeTypes.size > 1) {
                block.recipeTypes.forEach { recipeType ->
                    val type = recipeType.toString()
                    val subMatching = matching.filter { element -> element.type == type }
                    if (subMatching.isEmpty()) {
                        return@forEach
                    }
                    val subNode = TreeNodeModel()
                    subNode.count = subMatching.size
                    subMatching.forEach { element -> subNode.identifiers += element.uniqueId }
                    blockNode.children[type] = subNode
                }
            } else {
                matching.forEach { element -> blockNode.identifiers += element.uniqueId }
            }

            root.children[block.blockId.toString()] = blockNode
        }

        return root
    }

    fun folderIcons(): Map<String, Identifier> =
        LinkedHashMap<String, Identifier>().apply {
            RecipeTreeData.RECIPE_BLOCKS.forEach { block ->
                if (block.special) {
                    return@forEach
                }
                put(block.blockId.toString(), block.icon())
                block.recipeTypes.forEach { type -> put(type.toString(), block.icon()) }
            }
        }

    data class RecipeEntry(val uniqueId: String, val type: String)
}

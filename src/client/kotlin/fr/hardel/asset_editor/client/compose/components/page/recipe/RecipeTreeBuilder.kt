package fr.hardel.asset_editor.client.compose.components.page.recipe

import fr.hardel.asset_editor.client.compose.components.ui.tree.TreeNodeModel
import fr.hardel.asset_editor.client.compose.lib.data.RecipeTreeData
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

object RecipeTreeBuilder {

    fun build(elements: List<RecipeEntry>): TreeNodeModel {
        val root = TreeNodeModel()
        root.count = elements.size

        RecipeTreeData.RECIPE_ENTRIES.forEach { entry ->
            if (entry.special) {
                return@forEach
            }

            val typeStrings = entry.recipeTypes.map(Identifier::toString)
            val matching = elements.filter { element -> typeStrings.contains(element.type) }
            val entryNode = TreeNodeModel()
            entryNode.count = matching.size
            entryNode.label = I18n.get(entry.translationKey)

            if (entry.recipeTypes.size > 1) {
                entry.recipeTypes.forEach { recipeType ->
                    val type = recipeType.toString()
                    val subMatching = matching.filter { element -> element.type == type }
                    if (subMatching.isEmpty()) {
                        return@forEach
                    }
                    val subNode = TreeNodeModel()
                    subNode.count = subMatching.size
                    subNode.label = I18n.get("recipe:crafting.${recipeType.path}.name")
                    subMatching.forEach { element -> subNode.identifiers += element.uniqueId }
                    entryNode.children[type] = subNode
                }
            } else {
                matching.forEach { element -> entryNode.identifiers += element.uniqueId }
            }

            root.children[entry.entryId.toString()] = entryNode
        }

        return root
    }

    fun folderIcons(): Map<String, Identifier> =
        LinkedHashMap<String, Identifier>().apply {
            RecipeTreeData.RECIPE_ENTRIES.forEach { entry ->
                if (entry.special) {
                    return@forEach
                }
                put(entry.entryId.toString(), entry.folderIcon())
                entry.recipeTypes.forEach { type -> put(type.toString(), entry.folderIcon()) }
            }
        }

    data class RecipeEntry(val uniqueId: String, val type: String)
}

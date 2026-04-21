package fr.hardel.asset_editor.client.compose.components.page.recipe.utils

import fr.hardel.asset_editor.client.compose.components.page.recipe.RecipeTreeData
import fr.hardel.asset_editor.workspace.action.recipe.adapter.RecipeAdapterRegistry
import net.minecraft.resources.Identifier

data class RecipeVisualModel(
    val type: String,
    val slots: Map<String, List<String>>,
    val resultItemId: String,
    val resultCount: Int = 1,
    val resultCountEditable: Boolean = true,
    val resultCountMax: Int = 64,
    val properties: Map<String, Any?> = emptyMap()
) {
    inline fun <reified T> property(key: String): T? = properties[key] as? T
}

data class RecipeRuntimeEntry(
    val id: Identifier,
    val type: String,
    val serializer: String,
    val visual: RecipeVisualModel
)

fun placeholderRecipeVisual(type: String): RecipeVisualModel {
    val fallbackResult = RecipeTreeData.getEntryByRecipeType(type).assetId.toString()
    val resultCountEditable = Identifier.tryParse(type)?.let(RecipeAdapterRegistry::supportsResultCount) ?: false
    return RecipeVisualModel(
        type = type,
        slots = emptyMap(),
        resultItemId = fallbackResult,
        resultCountEditable = resultCountEditable
    )
}

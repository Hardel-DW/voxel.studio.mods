package fr.hardel.asset_editor.client.compose.components.page.recipe.utils

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

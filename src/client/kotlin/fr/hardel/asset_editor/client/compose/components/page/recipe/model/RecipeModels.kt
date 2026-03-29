package fr.hardel.asset_editor.client.compose.components.page.recipe.model

import net.minecraft.resources.Identifier

data class RecipeVisualModel(
    val type: String,
    val slots: Map<String, List<String>>,
    val resultItemId: String,
    val resultCount: Int = 1,
    val resultCountEditable: Boolean = true
)

data class RecipeRuntimeEntry(
    val id: Identifier,
    val type: String,
    val serializer: String,
    val visual: RecipeVisualModel
)

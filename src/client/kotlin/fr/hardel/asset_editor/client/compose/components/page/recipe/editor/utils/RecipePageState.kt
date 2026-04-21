package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils

import fr.hardel.asset_editor.client.compose.lib.StudioContext

data class RecipePageState(
    val editor: RecipeEditorState,
    val context: StudioContext,
    val recipeCounts: Map<String, Int>,
    val onSelectionChange: (String) -> Unit,
    val search: String,
    val onSearchChange: (String) -> Unit,
    val onSelectItem: (String) -> Unit,
    val onPaintReset: () -> Unit
)

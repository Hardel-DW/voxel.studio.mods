package fr.hardel.asset_editor.client.compose.components.page.recipe.utils

import fr.hardel.asset_editor.workspace.action.EditorAction

data class RecipeEditorState(
    val model: RecipeVisualModel,
    val selectedItemId: String?,
    val paintMode: PaintMode,
    val resultCountEnabled: Boolean,
    val onResultCountChange: (Int) -> Unit,
    val onResultItemChange: () -> Unit,
    val onAction: (EditorAction<*>) -> Unit
)

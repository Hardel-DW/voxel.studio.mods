package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils

import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.PaintMode
import fr.hardel.asset_editor.workspace.action.EditorAction

data class RecipeEditorState(
    val model: RecipeVisualModel,
    val selection: String,
    val recipeCounts: Map<String, Int>,
    val selectedItemId: String?,
    val paintMode: PaintMode,
    val resultCountEnabled: Boolean,
    val onSelectionChange: (String) -> Unit,
    val onResultCountChange: (Int) -> Unit,
    val onAction: (EditorAction) -> Unit
)
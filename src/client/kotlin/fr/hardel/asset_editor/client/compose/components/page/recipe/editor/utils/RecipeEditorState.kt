package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils

import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.PaintMode
import fr.hardel.asset_editor.workspace.action.EditorAction
import net.minecraft.world.item.crafting.Recipe

data class RecipeEditorState(
    val model: RecipeVisualModel,
    val recipe: Recipe<*>?,
    val selection: String,
    val recipeCounts: Map<String, Int>,
    val selectedItemId: String?,
    val paintMode: PaintMode,
    val resultCountEnabled: Boolean,
    val onSelectionChange: (String) -> Unit,
    val onResultCountChange: (Int) -> Unit,
    val onResultItemChange: () -> Unit,
    val onAction: (EditorAction<*>) -> Unit
)

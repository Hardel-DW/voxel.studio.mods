package fr.hardel.asset_editor.client.compose.components.page.recipe.utils

import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.PaintMode
import fr.hardel.asset_editor.workspace.action.EditorAction
import net.minecraft.world.item.crafting.Recipe

data class RecipeEditorState(
    val model: RecipeVisualModel,
    val recipe: Recipe<*>?,
    val selectedItemId: String?,
    val paintMode: PaintMode,
    val resultCountEnabled: Boolean,
    val onResultCountChange: (Int) -> Unit,
    val onResultItemChange: () -> Unit,
    val onAction: (EditorAction<*>) -> Unit
)

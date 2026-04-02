package fr.hardel.asset_editor.client.compose.components.page.recipe.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils.RecipeEditorState

enum class PaintMode { NONE, PAINTING, ERASING }

@Composable
fun RecipeEditorDispatch(
    state: RecipeEditorState,
    modifier: Modifier = Modifier
) {
    val editor = RecipeEditorRegistry.get(state.model.type) ?: { s, m -> FallbackEditor(s, m) }
    editor(state, modifier)
}

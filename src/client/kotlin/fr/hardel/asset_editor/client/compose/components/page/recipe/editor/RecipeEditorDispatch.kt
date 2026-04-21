package fr.hardel.asset_editor.client.compose.components.page.recipe.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils.RecipePageState

enum class PaintMode { NONE, PAINTING, ERASING }

@Composable
fun RecipeEditorDispatch(
    state: RecipePageState,
    modifier: Modifier = Modifier
) {
    val editor = RecipeEditorRegistry.get(state.editor.model.type) ?: { s, m -> FallbackEditor(s, m) }
    editor(state, modifier)
}

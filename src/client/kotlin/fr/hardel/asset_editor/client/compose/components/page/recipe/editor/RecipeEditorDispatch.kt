package fr.hardel.asset_editor.client.compose.components.page.recipe.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils.RecipeEditorState

typealias RecipeEditorFactory = @Composable (state: RecipeEditorState, modifier: Modifier) -> Unit

enum class PaintMode { NONE, PAINTING, ERASING }

private val EDITORS = mapOf<String, RecipeEditorFactory>(
    "minecraft:crafting_shaped" to { state, modifier -> CraftingShapedEditor(state, modifier) },
    "minecraft:crafting_shapeless" to { state, modifier -> CraftingShapelessEditor(state, modifier) },
    "minecraft:smelting" to { state, modifier -> CookingEditor(state, modifier) },
    "minecraft:blasting" to { state, modifier -> CookingEditor(state, modifier) },
    "minecraft:smoking" to { state, modifier -> CookingEditor(state, modifier) },
    "minecraft:campfire_cooking" to { state, modifier -> CookingEditor(state, modifier) },
    "minecraft:stonecutting" to { state, modifier -> StonecutterEditor(state, modifier) },
    "minecraft:smithing_transform" to { state, modifier -> SmithingTransformEditor(state, modifier) },
    "minecraft:smithing_trim" to { state, modifier -> SmithingTrimEditor(state, modifier) },
    "minecraft:crafting_transmute" to { state, modifier -> TransmuteEditor(state, modifier) }
)

@Composable
fun RecipeEditorDispatch(
    state: RecipeEditorState,
    modifier: Modifier = Modifier
) {
    val editor = EDITORS[state.model.type] ?: { s, m -> FallbackEditor(s, m) }
    editor(state, modifier)
}

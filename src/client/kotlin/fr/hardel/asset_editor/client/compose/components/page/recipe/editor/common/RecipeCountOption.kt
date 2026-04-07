package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common

import androidx.compose.runtime.Composable
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils.RecipeEditorState
import net.minecraft.client.resources.language.I18n

@Composable
fun RecipeCountOption(state: RecipeEditorState) {
    if (!state.model.resultCountEditable) return

    EditorCard {
        CounterOptionRow(
            title = I18n.get("recipe:section.result_count"),
            description = if (!state.resultCountEnabled && state.model.resultCountMax == 1) {
                I18n.get("recipe:section.result_count_locked")
            } else {
                I18n.get("recipe:section.result_count_description")
            },
            value = state.model.resultCount,
            max = state.model.resultCountMax,
            enabled = state.resultCountEnabled,
            onValueChange = state.onResultCountChange
        )
    }
}

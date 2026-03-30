package fr.hardel.asset_editor.client.compose.components.page.recipe.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.EditorCard
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils.RecipeEditorState
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.RecipeSection
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.ResultCountOption
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils.slotRemoveAction
import fr.hardel.asset_editor.client.compose.components.page.recipe.template.CraftingTemplate
import fr.hardel.asset_editor.workspace.action.recipe.RecipeEditorActions
import net.minecraft.resources.Identifier

@Composable
fun CraftingShapelessEditor(state: RecipeEditorState, modifier: Modifier = Modifier) {
    fun addShapeless() {
        val itemId = state.selectedItemId ?: return
        val itemIdentifier = Identifier.tryParse(itemId) ?: return
        state.onAction(RecipeEditorActions.AddShapelessIngredient(listOf(itemIdentifier)))
    }

    RecipeSection(
        selection = state.selection,
        recipeCounts = state.recipeCounts,
        onSelectionChange = state.onSelectionChange,
        modifier = modifier
    ) {
        CraftingTemplate(
            slots = state.model.slots,
            resultItemId = state.model.resultItemId,
            resultCount = state.model.resultCount,
            interactive = true,
            onSlotPointerDown = { slot, button ->
                when (button) {
                    PointerButton.Primary -> addShapeless()
                    PointerButton.Secondary -> slotRemoveAction(slot)?.let(state.onAction)
                    else -> {}
                }
            },
            onSlotPointerEnter = { slot ->
                when (state.paintMode) {
                    PaintMode.PAINTING -> addShapeless()
                    PaintMode.ERASING -> slotRemoveAction(slot)?.let(state.onAction)
                    PaintMode.NONE -> {}
                }
            }
        )

        EditorCard {
            ResultCountOption(
                value = state.model.resultCount,
                max = state.model.resultCountMax,
                enabled = state.resultCountEnabled,
                onValueChange = state.onResultCountChange
            )
        }
    }
}

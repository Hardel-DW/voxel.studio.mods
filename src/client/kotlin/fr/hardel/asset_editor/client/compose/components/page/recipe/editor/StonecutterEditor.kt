package fr.hardel.asset_editor.client.compose.components.page.recipe.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.EditorCard
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.RecipeCountOption
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.RecipeGroupOption
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils.RecipeEditorState
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.RecipeSection
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils.slotAddAction
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils.slotPointerDownAction
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils.slotRemoveAction
import fr.hardel.asset_editor.client.compose.components.page.recipe.template.StoneCuttingTemplate
import fr.hardel.asset_editor.workspace.action.recipe.RecipeEditorActions
import net.minecraft.world.item.crafting.StonecutterRecipe

@Composable
fun StonecutterEditor(state: RecipeEditorState, modifier: Modifier = Modifier) {
    val recipe = state.recipe as? StonecutterRecipe

    RecipeSection(
        selection = state.selection,
        recipeCounts = state.recipeCounts,
        onSelectionChange = state.onSelectionChange,
        modifier = modifier
    ) {
        StoneCuttingTemplate(
            slots = state.model.slots,
            resultItemId = state.model.resultItemId,
            resultCount = state.model.resultCount,
            interactive = true,
            onSlotPointerDown = { slot, button ->
                slotPointerDownAction(slot, button, state.selectedItemId)?.let(state.onAction)
            },
            onSlotPointerEnter = { slot ->
                when (state.paintMode) {
                    PaintMode.PAINTING -> slotAddAction(slot, state.selectedItemId)?.let(state.onAction)
                    PaintMode.ERASING -> slotRemoveAction(slot)?.let(state.onAction)
                    PaintMode.NONE -> {}
                }
            },
            onResultPointerDown = { button ->
                if (button == PointerButton.Primary) {
                    state.onResultItemChange()
                }
            },
            onResultPointerEnter = {
                if (state.paintMode == PaintMode.PAINTING) {
                    state.onResultItemChange()
                }
            }
        )

        RecipeCountOption(state)

        recipe?.let {
            EditorCard {
                RecipeGroupOption(
                    value = it.group(),
                    onValueChange = { value -> state.onAction(RecipeEditorActions.SetGroup(value)) }
                )
            }
        }
    }
}

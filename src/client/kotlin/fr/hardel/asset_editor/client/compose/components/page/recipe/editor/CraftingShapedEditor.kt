package fr.hardel.asset_editor.client.compose.components.page.recipe.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.CounterOptionRow
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.EditorCard
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.RecipeCategoryOption
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.RecipeGroupOption
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils.RecipeEditorState
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.RecipeSection
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.RecipeShowNotificationOption
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils.slotAddAction
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils.slotPointerDownAction
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils.slotRemoveAction
import fr.hardel.asset_editor.client.compose.components.page.recipe.template.CraftingTemplate
import fr.hardel.asset_editor.workspace.action.recipe.RecipeEditorActions
import net.minecraft.client.resources.language.I18n
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.crafting.ShapedRecipe

@Composable
fun CraftingShapedEditor(state: RecipeEditorState, modifier: Modifier = Modifier) {
    val recipe = state.recipe as? ShapedRecipe

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

        if (state.model.resultCountEditable) {
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

        recipe?.let {
            EditorCard {
                RecipeGroupOption(
                    value = it.group(),
                    onValueChange = { value -> state.onAction(RecipeEditorActions.SetGroup(value)) }
                )
            }

            EditorCard {
                RecipeCategoryOption(
                    value = it.category().serializedName,
                    options = CraftingBookCategory.entries.map { category -> category.serializedName },
                    onValueChange = { value -> state.onAction(RecipeEditorActions.SetCategory(value)) }
                )
            }

            EditorCard {
                RecipeShowNotificationOption(
                    value = it.showNotification(),
                    onValueChange = { value -> state.onAction(RecipeEditorActions.SetShowNotification(value)) }
                )
            }
        }
    }
}

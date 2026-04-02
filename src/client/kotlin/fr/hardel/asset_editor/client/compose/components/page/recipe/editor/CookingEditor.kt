package fr.hardel.asset_editor.client.compose.components.page.recipe.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.RecipeCategoryOption
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.RecipeCookingTimeOption
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.RecipeCountOption
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.EditorCard
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.RecipeExperienceOption
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.RecipeGroupOption
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils.RecipeEditorState
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.RecipeSection
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils.slotAddAction
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils.slotPointerDownAction
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils.slotRemoveAction
import fr.hardel.asset_editor.client.compose.components.page.recipe.template.SmeltingTemplate
import fr.hardel.asset_editor.workspace.action.recipe.RecipeEditorActions
import net.minecraft.world.item.crafting.AbstractCookingRecipe
import net.minecraft.world.item.crafting.CampfireCookingRecipe
import net.minecraft.world.item.crafting.CookingBookCategory

@Composable
fun CookingEditor(state: RecipeEditorState, modifier: Modifier = Modifier) {
    val recipe = state.recipe as? AbstractCookingRecipe

    RecipeSection(
        selection = state.selection,
        recipeCounts = state.recipeCounts,
        onSelectionChange = state.onSelectionChange,
        modifier = modifier
    ) {
        SmeltingTemplate(
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

            EditorCard {
                RecipeCategoryOption(
                    value = it.category().serializedName,
                    options = CookingBookCategory.entries.map { category -> category.serializedName },
                    onValueChange = { value -> state.onAction(RecipeEditorActions.SetCategory(value)) }
                )
            }

            EditorCard {
                RecipeExperienceOption(
                    value = it.experience(),
                    onValueChange = { value -> state.onAction(RecipeEditorActions.SetCookingExperience(value)) }
                )
            }

            EditorCard {
                RecipeCookingTimeOption(
                    value = it.cookingTime(),
                    max = if (it is CampfireCookingRecipe) Int.MAX_VALUE else Short.MAX_VALUE.toInt(),
                    onValueChange = { value -> state.onAction(RecipeEditorActions.SetCookingTime(value)) }
                )
            }
        }
    }
}

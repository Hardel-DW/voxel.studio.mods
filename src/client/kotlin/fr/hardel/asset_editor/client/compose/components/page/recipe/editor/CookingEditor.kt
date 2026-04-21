package fr.hardel.asset_editor.client.compose.components.page.recipe.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.RecipeAdvancedOptions
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.RecipeCategoryOption
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.RecipeCookingTimeOption
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.RecipeCountOption
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.EditorCard
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.RecipeExperienceOption
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.RecipeGroupOption
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.RecipePageLayout
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils.RecipePageState
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils.slotAddAction
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils.slotPointerDownAction
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils.slotRemoveAction
import fr.hardel.asset_editor.client.compose.components.page.recipe.template.SmeltingTemplate
import fr.hardel.asset_editor.workspace.action.recipe.SetCategoryAction
import fr.hardel.asset_editor.workspace.action.recipe.SetCookingExperienceAction
import fr.hardel.asset_editor.workspace.action.recipe.SetCookingTimeAction
import fr.hardel.asset_editor.workspace.action.recipe.SetGroupAction
import net.minecraft.world.item.crafting.AbstractCookingRecipe
import net.minecraft.world.item.crafting.CampfireCookingRecipe
import net.minecraft.world.item.crafting.CookingBookCategory

@Composable
fun CookingEditor(state: RecipePageState, modifier: Modifier = Modifier) {
    val recipe = state.editor.recipe as? AbstractCookingRecipe
    val s = state.editor

    RecipePageLayout(state = state, modifier = modifier) {
        SmeltingTemplate(
            slots = s.model.slots,
            resultItemId = s.model.resultItemId,
            resultCount = s.model.resultCount,
            interactive = true,
            onSlotPointerDown = { slot, button ->
                slotPointerDownAction(slot, button, s.selectedItemId)?.let(s.onAction)
            },
            onSlotPointerEnter = { slot ->
                when (s.paintMode) {
                    PaintMode.PAINTING -> slotAddAction(slot, s.selectedItemId)?.let(s.onAction)
                    PaintMode.ERASING -> slotRemoveAction(slot)?.let(s.onAction)
                    PaintMode.NONE -> {}
                }
            },
            onResultPointerDown = { button ->
                if (button == PointerButton.Primary) s.onResultItemChange()
            },
            onResultPointerEnter = {
                if (s.paintMode == PaintMode.PAINTING) s.onResultItemChange()
            }
        )

        RecipeCountOption(s)

        recipe?.let {
            RecipeAdvancedOptions {
                EditorCard {
                    RecipeGroupOption(
                        value = it.group(),
                        onValueChange = { value -> s.onAction(SetGroupAction(value)) }
                    )
                }
                EditorCard {
                    RecipeCategoryOption(
                        value = it.category().serializedName,
                        options = CookingBookCategory.entries.map { category -> category.serializedName },
                        onValueChange = { value -> s.onAction(SetCategoryAction(value)) }
                    )
                }
                EditorCard {
                    RecipeExperienceOption(
                        value = it.experience(),
                        onValueChange = { value -> s.onAction(SetCookingExperienceAction(value)) }
                    )
                }
                EditorCard {
                    RecipeCookingTimeOption(
                        value = it.cookingTime(),
                        max = if (it is CampfireCookingRecipe) Int.MAX_VALUE else Short.MAX_VALUE.toInt(),
                        onValueChange = { value -> s.onAction(SetCookingTimeAction(value)) }
                    )
                }
            }
        }
    }
}

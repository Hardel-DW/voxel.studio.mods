package fr.hardel.asset_editor.client.compose.components.page.recipe.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.options.RecipeAdvancedOptions
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.options.RecipeCategoryOption
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.options.RecipeCountOption
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.EditorCard
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.options.RecipeGroupOption
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.RecipePageLayout
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.QuickSwapTabs
import fr.hardel.asset_editor.client.compose.components.page.recipe.utils.RecipePageState
import fr.hardel.asset_editor.client.compose.components.page.recipe.utils.slotRemoveAction
import fr.hardel.asset_editor.client.compose.components.page.recipe.template.CraftingTemplate
import fr.hardel.asset_editor.workspace.action.recipe.AddShapelessIngredientAction
import fr.hardel.asset_editor.workspace.action.recipe.SetCategoryAction
import fr.hardel.asset_editor.workspace.action.recipe.SetGroupAction
import net.minecraft.resources.Identifier
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.crafting.ShapelessRecipe

@Composable
fun CraftingShapelessEditor(state: RecipePageState, modifier: Modifier = Modifier) {
    val recipe = state.editor.recipe as? ShapelessRecipe
    val s = state.editor

    fun addShapeless() {
        val itemId = s.selectedItemId ?: return
        val itemIdentifier = Identifier.tryParse(itemId) ?: return
        s.onAction(AddShapelessIngredientAction(listOf(itemIdentifier)))
    }

    RecipePageLayout(
        state = state,
        modifier = modifier,
        headerExtra = { QuickSwapTabs(state) }
    ) {
        CraftingTemplate(
            slots = s.model.slots,
            resultItemId = s.model.resultItemId,
            resultCount = s.model.resultCount,
            interactive = true,
            onSlotPointerDown = { slot, button ->
                when (button) {
                    PointerButton.Primary -> addShapeless()
                    PointerButton.Secondary -> slotRemoveAction(slot)?.let(s.onAction)
                    else -> {}
                }
            },
            onSlotPointerEnter = { slot ->
                when (s.paintMode) {
                    PaintMode.PAINTING -> addShapeless()
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
                        options = CraftingBookCategory.entries.map { category -> category.serializedName },
                        onValueChange = { value -> s.onAction(SetCategoryAction(value)) }
                    )
                }
            }
        }
    }
}

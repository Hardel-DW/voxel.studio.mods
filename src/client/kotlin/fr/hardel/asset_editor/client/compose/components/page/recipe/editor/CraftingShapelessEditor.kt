package fr.hardel.asset_editor.client.compose.components.page.recipe.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.EditorCard
import fr.hardel.asset_editor.client.compose.components.ui.AnimatedTabs
import net.minecraft.client.resources.language.I18n
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.RecipeInventory
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.RecipeSectionCard
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.RecipeSectionHeader
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.RecipeSelector
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.ResultComponentsSection
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.options.RecipeAdvancedOptions
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.options.RecipeCategoryOption
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.options.RecipeCountOption
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.options.RecipeGroupOption
import fr.hardel.asset_editor.client.compose.components.page.recipe.template.CraftingTemplate
import fr.hardel.asset_editor.workspace.action.recipe.adapter.ShapelessRecipeAdapter
import fr.hardel.asset_editor.client.compose.components.page.recipe.utils.PaintMode
import fr.hardel.asset_editor.client.compose.components.page.recipe.utils.RecipePageState
import fr.hardel.asset_editor.client.compose.components.page.recipe.utils.slotAddAction
import fr.hardel.asset_editor.client.compose.components.page.recipe.utils.slotPointerDownAction
import fr.hardel.asset_editor.client.compose.components.page.recipe.utils.slotRemoveAction
import fr.hardel.asset_editor.workspace.action.recipe.SetCategoryAction
import fr.hardel.asset_editor.workspace.action.recipe.SetGroupAction
import net.minecraft.world.item.crafting.CraftingBookCategory

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CraftingShapelessEditor(state: RecipePageState, modifier: Modifier = Modifier) {
    val s = state.editor

    Row(
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxSize()
            .onPointerEvent(PointerEventType.Release) { state.onPaintReset() }
    ) {
        RecipeSectionCard(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                RecipeSectionHeader(recipeType = s.model.type, modifier = Modifier.weight(1f))
                AnimatedTabs(
                    options = linkedMapOf(
                        "minecraft:crafting_shaped" to I18n.get("recipe:crafting.crafting_shaped.name"),
                        "minecraft:crafting_shapeless" to I18n.get("recipe:crafting.crafting_shapeless.name")
                    ),
                    selectedValue = s.model.type,
                    onValueChange = state.onSelectionChange
                )
                RecipeSelector(
                    value = s.model.type,
                    onChange = state.onSelectionChange,
                    recipeCounts = state.recipeCounts,
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                CraftingTemplate(
                    slots = s.model.slots,
                    resultItemId = s.model.resultItemId,
                    resultCount = s.model.resultCount,
                    interactive = true,
                    onSlotPointerDown = { slot, button ->
                        slotPointerDownAction(slot, button, s.selectedItemId, s.model.slots)?.let(s.onAction)
                    },
                    onSlotPointerEnter = { slot ->
                        when (s.paintMode.value) {
                            PaintMode.PAINTING -> slotAddAction(slot, s.selectedItemId)?.let(s.onAction)
                            PaintMode.ERASING -> slotRemoveAction(slot, s.model.slots)?.let(s.onAction)
                            PaintMode.NONE -> {}
                        }
                    },
                    onResultPointerDown = { button ->
                        if (button == PointerButton.Primary) s.onResultItemChange()
                    },
                    onResultPointerEnter = {
                        if (s.paintMode.value == PaintMode.PAINTING) s.onResultItemChange()
                    }
                )

                RecipeCountOption(s)

                RecipeAdvancedOptions {
                    EditorCard {
                        RecipeGroupOption(
                            value = s.model.property<String>(ShapelessRecipeAdapter.GROUP) ?: "",
                            onValueChange = { value -> s.onAction(SetGroupAction(value)) }
                        )
                    }
                    s.model.property<String>(ShapelessRecipeAdapter.CATEGORY)?.let { category ->
                        EditorCard {
                            RecipeCategoryOption(
                                value = category,
                                options = CraftingBookCategory.entries.map { it.serializedName },
                                onValueChange = { value -> s.onAction(SetCategoryAction(value)) }
                            )
                        }
                    }
                }

                ResultComponentsSection(
                    context = state.context,
                    onAction = s.onAction
                )
            }
        }

        RecipeInventory(
            context = state.context,
            search = state.search,
            onSearchChange = state.onSearchChange,
            selectedItemId = s.selectedItemId,
            onSelectItem = state.onSelectItem,
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
    }
}

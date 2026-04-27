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
import fr.hardel.asset_editor.client.compose.components.page.recipe.RecipeTreeData
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.RecipeInventory
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.RecipeSectionCard
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.RecipeSectionHeader
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.RecipeSelector
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.options.RecipeCountOption
import fr.hardel.asset_editor.client.compose.components.page.recipe.template.RecipeTemplateRegistry
import fr.hardel.asset_editor.client.compose.components.page.recipe.utils.PaintMode
import fr.hardel.asset_editor.client.compose.components.page.recipe.utils.RecipePageState
import fr.hardel.asset_editor.client.compose.components.page.recipe.utils.eraseSlotEdit
import fr.hardel.asset_editor.client.compose.components.page.recipe.utils.paintSlotEdit
import fr.hardel.asset_editor.client.compose.components.page.recipe.utils.pointerDownSlotEdit

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FallbackEditor(state: RecipePageState, modifier: Modifier = Modifier) {
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
                RecipeTemplateRegistry.Render(
                    kind = RecipeTreeData.getTemplateKind(s.model.type),
                    element = s.model,
                    interactive = true,
                    onSlotPointerDown = { slot, button ->
                        pointerDownSlotEdit(slot, button, s.selectedItemId, s.model.slots)?.let(s.onSlotEdit)
                    },
                    onSlotPointerEnter = { slot ->
                        when (s.paintMode.value) {
                            PaintMode.PAINTING -> paintSlotEdit(slot, s.selectedItemId, s.model.slots)?.let(s.onSlotEdit)
                            PaintMode.ERASING -> eraseSlotEdit(slot, s.model.slots)?.let(s.onSlotEdit)
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

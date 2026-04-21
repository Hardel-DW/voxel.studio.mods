package fr.hardel.asset_editor.client.compose.components.page.recipe.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.RecipePageLayout
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils.RecipePageState
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils.slotAddAction
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils.slotPointerDownAction
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils.slotRemoveAction
import fr.hardel.asset_editor.client.compose.components.page.recipe.template.SmithingTemplate

@Composable
fun SmithingTrimEditor(state: RecipePageState, modifier: Modifier = Modifier) {
    val s = state.editor

    RecipePageLayout(state = state, modifier = modifier) {
        SmithingTemplate(
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
    }
}

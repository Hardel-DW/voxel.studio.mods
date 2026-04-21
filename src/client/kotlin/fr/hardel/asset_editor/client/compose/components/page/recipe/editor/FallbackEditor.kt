package fr.hardel.asset_editor.client.compose.components.page.recipe.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import fr.hardel.asset_editor.client.compose.components.page.recipe.RecipeTreeData
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.options.RecipeCountOption
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.RecipePageLayout
import fr.hardel.asset_editor.client.compose.components.page.recipe.utils.RecipePageState
import fr.hardel.asset_editor.client.compose.components.page.recipe.utils.slotAddAction
import fr.hardel.asset_editor.client.compose.components.page.recipe.utils.slotPointerDownAction
import fr.hardel.asset_editor.client.compose.components.page.recipe.utils.slotRemoveAction
import fr.hardel.asset_editor.client.compose.components.page.recipe.template.RecipeTemplateRegistry

@Composable
fun FallbackEditor(state: RecipePageState, modifier: Modifier = Modifier) {
    val s = state.editor

    RecipePageLayout(state = state, modifier = modifier) {
        RecipeTemplateRegistry.Render(
            kind = RecipeTreeData.getTemplateKind(s.model.type),
            element = s.model,
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
    }
}

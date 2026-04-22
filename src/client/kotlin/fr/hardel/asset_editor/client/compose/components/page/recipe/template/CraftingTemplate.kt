package fr.hardel.asset_editor.client.compose.components.page.recipe.template

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton

@Composable
fun CraftingTemplate(
    slots: Map<String, List<String>>,
    resultItemId: String,
    resultCount: Int,
    modifier: Modifier = Modifier,
    interactive: Boolean = false,
    onSlotPointerDown: ((String, PointerButton) -> Unit)? = null,
    onSlotPointerEnter: ((String) -> Unit)? = null,
    onResultPointerDown: ((PointerButton) -> Unit)? = null,
    onResultPointerEnter: (() -> Unit)? = null
) {
    RecipeTemplateBase(
        resultItemId = resultItemId,
        resultCount = resultCount,
        modifier = modifier,
        interactiveResult = interactive,
        onResultPointerDown = onResultPointerDown,
        onResultPointerEnter = onResultPointerEnter
    ) {
        Box {
            SlotGrid(
                columns = 3, rows = 3,
                slots = slots,
                interactive = interactive,
                onSlotPointerDown = onSlotPointerDown,
                onSlotPointerEnter = onSlotPointerEnter
            )
        }
    }
}

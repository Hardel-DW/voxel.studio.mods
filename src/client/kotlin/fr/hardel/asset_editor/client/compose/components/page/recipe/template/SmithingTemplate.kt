package fr.hardel.asset_editor.client.compose.components.page.recipe.template

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.components.page.recipe.RecipeSlot
import fr.hardel.asset_editor.client.compose.components.page.recipe.template.RecipeTemplateBase

@Composable
fun SmithingTemplate(
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
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (slotIndex in 0..2) {
                val index = slotIndex.toString()
                RecipeSlot(
                    slotIndex = index,
                    item = slots[index].orEmpty(),
                    interactive = interactive,
                    onPointerDown = onSlotPointerDown?.let { cb -> { button -> cb(index, button) } },
                    onPointerEnter = onSlotPointerEnter?.let { cb -> { cb(index) } }
                )
            }
        }
    }
}

package fr.hardel.asset_editor.client.compose.components.page.recipe.template

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.components.page.recipe.RecipeSlot
import fr.hardel.asset_editor.client.compose.components.page.recipe.template.RecipeTemplateBase

@Composable
fun CraftingTemplate(
    slots: Map<String, List<String>>,
    resultItemId: String,
    resultCount: Int,
    modifier: Modifier = Modifier,
    interactive: Boolean = false,
    onSlotPointerDown: ((String, PointerButton) -> Unit)? = null,
    onSlotPointerEnter: ((String) -> Unit)? = null
) {
    RecipeTemplateBase(
        resultItemId = resultItemId,
        resultCount = resultCount,
        modifier = modifier
    ) {
        Box {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(3) { column ->
                            val index = (row * 3 + column).toString()
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
        }
    }
}

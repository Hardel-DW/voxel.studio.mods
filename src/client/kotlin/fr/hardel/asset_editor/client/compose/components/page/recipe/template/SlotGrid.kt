package fr.hardel.asset_editor.client.compose.components.page.recipe.template

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.components.page.recipe.RecipeSlot

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SlotGrid(
    columns: Int,
    rows: Int = 1,
    slots: Map<String, List<String>>,
    interactive: Boolean = false,
    onSlotPointerDown: ((String, PointerButton) -> Unit)? = null,
    onSlotPointerEnter: ((String) -> Unit)? = null
) {
    val density = LocalDensity.current
    var lastDragSlot by remember { mutableStateOf<String?>(null) }
    val currentOnEnter by rememberUpdatedState(onSlotPointerEnter)

    val size = with(density) { 48.dp.toPx() }
    val pitch = size + with(density) { 4.dp.toPx() }

    fun slotAt(x: Float, y: Float): String? {
        val c = (x / pitch).toInt(); val r = (y / pitch).toInt()
        if (c !in 0 until columns || r !in 0 until rows) return null
        if (x - c * pitch >= size || y - r * pitch >= size) return null
        return (r * columns + c).toString()
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = if (interactive) Modifier
            .onPointerEvent(PointerEventType.Press) { event ->
                val p = event.changes.first().position
                slotAt(p.x, p.y)?.let { lastDragSlot = it }
            }
            .onPointerEvent(PointerEventType.Move) { event ->
                if (event.changes.none { it.pressed }) { lastDragSlot = null; return@onPointerEvent }
                val p = event.changes.first().position
                slotAt(p.x, p.y)?.let { slot ->
                    if (slot != lastDragSlot) { lastDragSlot = slot; currentOnEnter?.invoke(slot) }
                }
            }
        else Modifier
    ) {
        repeat(rows) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(columns) { col ->
                    val index = (row * columns + col).toString()
                    RecipeSlot(
                        slotIndex = index,
                        item = slots[index].orEmpty(),
                        interactive = interactive,
                        onPointerDown = onSlotPointerDown?.let { cb -> { button -> cb(index, button) } }
                    )
                }
            }
        }
    }
}

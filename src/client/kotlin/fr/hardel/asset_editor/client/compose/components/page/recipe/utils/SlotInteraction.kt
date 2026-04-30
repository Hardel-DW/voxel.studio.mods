package fr.hardel.asset_editor.client.compose.components.page.recipe.utils

import androidx.compose.ui.input.pointer.PointerButton
import net.minecraft.resources.Identifier

enum class SlotEditKind { PAINT, ERASE }

data class SlotEdit(val slots: Map<String, List<String>>, val kind: SlotEditKind) {
    fun toServerSlots(): Map<Int, List<Identifier>> = slots.entries.associate { (key, items) ->
        key.toInt() to items.mapNotNull(Identifier::tryParse)
    }
}

fun pointerDownSlotEdit(
    slot: String,
    button: PointerButton,
    selectedItemId: String?,
    currentSlots: Map<String, List<String>>
): SlotEdit? = when (button) {
    PointerButton.Primary -> {
        val itemId = selectedItemId
        if (itemId != null && currentSlots[slot]?.singleOrNull() == itemId)
            eraseSlot(slot, currentSlots)
        else
            paintSlot(slot, itemId, currentSlots)
    }
    PointerButton.Secondary -> eraseSlot(slot, currentSlots)
    else -> null
}

fun paintSlotEdit(slot: String, selectedItemId: String?, currentSlots: Map<String, List<String>>): SlotEdit? =
    paintSlot(slot, selectedItemId, currentSlots)

fun eraseSlotEdit(slot: String, currentSlots: Map<String, List<String>>): SlotEdit? =
    eraseSlot(slot, currentSlots)

private fun paintSlot(slot: String, itemId: String?, current: Map<String, List<String>>): SlotEdit? {
    val item = itemId ?: return null
    if (current[slot]?.singleOrNull() == item) return null
    return SlotEdit(current + (slot to listOf(item)), SlotEditKind.PAINT)
}

private fun eraseSlot(slot: String, current: Map<String, List<String>>): SlotEdit? {
    if (slot !in current) return null
    return SlotEdit(current - slot, SlotEditKind.ERASE)
}

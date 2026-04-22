package fr.hardel.asset_editor.client.compose.components.page.recipe.utils

import androidx.compose.ui.input.pointer.PointerButton
import fr.hardel.asset_editor.workspace.action.EditorAction
import fr.hardel.asset_editor.workspace.action.recipe.AddIngredientAction
import fr.hardel.asset_editor.workspace.action.recipe.RemoveIngredientAction
import net.minecraft.resources.Identifier

fun slotAddAction(slot: String, selectedItemId: String?): EditorAction<*>? {
    val itemId = selectedItemId ?: return null
    val itemIdentifier = Identifier.tryParse(itemId) ?: return null
    val slotIndex = slot.toIntOrNull() ?: return null
    return AddIngredientAction(slotIndex, listOf(itemIdentifier), true)
}

fun slotRemoveAction(slot: String, currentSlots: Map<String, List<String>>): EditorAction<*>? {
    val slotIndex = slot.toIntOrNull() ?: return null
    val items = currentSlots[slot]?.mapNotNull { Identifier.tryParse(it) } ?: emptyList()
    return RemoveIngredientAction(slotIndex, items)
}

fun slotPointerDownAction(
    slot: String,
    button: PointerButton,
    selectedItemId: String?,
    currentSlots: Map<String, List<String>>
): EditorAction<*>? {
    return when (button) {
        PointerButton.Primary -> {
            val itemId = selectedItemId ?: return null
            val currentItems = currentSlots[slot].orEmpty()
            if (currentItems.singleOrNull() == itemId) {
                slotRemoveAction(slot, currentSlots)
            } else {
                slotAddAction(slot, selectedItemId)
            }
        }
        PointerButton.Secondary -> slotRemoveAction(slot, currentSlots)
        else -> null
    }
}

fun applySlotEdit(
    slots: Map<String, List<String>>,
    action: EditorAction<*>
): Map<String, List<String>>? = when (action) {
    is AddIngredientAction -> {
        val items = action.items.map { it.toString() }
        if (items.isEmpty()) null
        else slots + (action.slot.toString() to items)
    }
    is RemoveIngredientAction -> {
        val result = slots - action.slot.toString()
        if (result.isEmpty()) slots else result
    }
    else -> null
}

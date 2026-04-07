package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils

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

fun slotRemoveAction(slot: String): EditorAction<*>? {
    val slotIndex = slot.toIntOrNull() ?: return null
    return RemoveIngredientAction(slotIndex, emptyList())
}

fun slotPointerDownAction(slot: String, button: PointerButton, selectedItemId: String?): EditorAction<*>? {
    return when (button) {
        PointerButton.Primary -> slotAddAction(slot, selectedItemId)
        PointerButton.Secondary -> slotRemoveAction(slot)
        else -> null
    }
}

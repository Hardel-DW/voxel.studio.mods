package fr.hardel.asset_editor.client

import fr.hardel.asset_editor.client.compose.components.page.recipe.utils.applySlotEdit
import fr.hardel.asset_editor.client.compose.components.page.recipe.utils.slotAddAction
import fr.hardel.asset_editor.client.compose.components.page.recipe.utils.slotRemoveAction
import fr.hardel.asset_editor.workspace.action.recipe.AddIngredientAction
import fr.hardel.asset_editor.workspace.action.recipe.RemoveIngredientAction
import net.minecraft.resources.Identifier
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SlotInteractionTest {

    private val planks = "minecraft:acacia_planks"
    private val stone = "minecraft:stone"

    @Test
    fun addIngredientPlacesAtClickedSlot() {
        val slots = emptyMap<String, List<String>>()
        val action = slotAddAction("4", planks)!!
        val result = applySlotEdit(slots, action)!!

        assertEquals(mapOf("4" to listOf(planks)), result)
    }

    @Test
    fun addIngredientPreservesExistingSlots() {
        val slots = mapOf("0" to listOf(planks), "1" to listOf(planks))
        val action = slotAddAction("5", stone)!!
        val result = applySlotEdit(slots, action)!!

        assertEquals(3, result.size)
        assertEquals(listOf(planks), result["0"])
        assertEquals(listOf(planks), result["1"])
        assertEquals(listOf(stone), result["5"])
    }

    @Test
    fun removeIngredientPassesSlotContent() {
        val slots = mapOf("0" to listOf(planks), "3" to listOf(stone))
        val action = slotRemoveAction("3", slots)!!

        assertTrue(action is RemoveIngredientAction)
        val removeAction = action as RemoveIngredientAction
        assertEquals(listOf(Identifier.parse(stone)), removeAction.items)

        val result = applySlotEdit(slots, action)!!
        assertEquals(mapOf("0" to listOf(planks)), result)
    }

    @Test
    fun removeFromEmptySlotPassesEmptyItems() {
        val slots = mapOf("0" to listOf(planks))
        val action = slotRemoveAction("5", slots)!!

        assertTrue(action is RemoveIngredientAction)
        val removeAction = action as RemoveIngredientAction
        assertTrue(removeAction.items.isEmpty())
    }

    @Test
    fun addReturnsNullWithNoSelectedItem() {
        assertNull(slotAddAction("0", null))
    }

    @Test
    fun shapelessDragSimulation_correctSlotPositions() {
        var slots = mapOf("0" to listOf(planks), "1" to listOf(planks))

        val click = slotAddAction("2", planks)!!
        slots = applySlotEdit(slots, click)!!

        val drag5 = slotAddAction("5", planks)!!
        slots = applySlotEdit(slots, drag5)!!

        val drag8 = slotAddAction("8", planks)!!
        slots = applySlotEdit(slots, drag8)!!

        assertEquals(5, slots.size, "Visual should have 5 slots")
        assertTrue(slots.containsKey("0"))
        assertTrue(slots.containsKey("1"))
        assertTrue(slots.containsKey("2"))
        assertTrue(slots.containsKey("5"))
        assertTrue(slots.containsKey("8"))
    }

    @Test
    fun duplicateAddOnSameSlotOverwritesVisually() {
        var slots = mapOf("0" to listOf(planks))
        val add1 = slotAddAction("3", stone)!!
        slots = applySlotEdit(slots, add1)!!
        val add2 = slotAddAction("3", stone)!!
        slots = applySlotEdit(slots, add2)!!

        assertEquals(2, slots.size, "Duplicate visual add should not create extra entries")
    }

    @Test
    fun removeLastIngredientKeepsItVisually() {
        var slots = mapOf("4" to listOf(planks))
        val remove = slotRemoveAction("4", slots)!!
        val result = applySlotEdit(slots, remove)!!

        assertEquals(1, result.size, "Cannot visually remove the last ingredient (Minecraft requires at least 1)")
        assertEquals(listOf(planks), result["4"])
    }

    @Test
    fun removeNonLastIngredientWorks() {
        var slots = mapOf("0" to listOf(planks), "4" to listOf(stone))
        val remove = slotRemoveAction("4", slots)!!
        val result = applySlotEdit(slots, remove)!!

        assertEquals(1, result.size)
        assertEquals(listOf(planks), result["0"])
        assertFalse(result.containsKey("4"))
    }

    @Test
    fun addIngredientCreatesCorrectServerAction() {
        val action = slotAddAction("7", planks)!!
        assertTrue(action is AddIngredientAction)
        val addAction = action as AddIngredientAction
        assertEquals(7, addAction.slot)
        assertEquals(listOf(Identifier.parse(planks)), addAction.items)
        assertTrue(addAction.replace)
    }
}

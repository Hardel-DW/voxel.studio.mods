package fr.hardel.asset_editor.client

import fr.hardel.asset_editor.client.compose.components.page.recipe.utils.SlotEditKind
import fr.hardel.asset_editor.client.compose.components.page.recipe.utils.eraseSlotEdit
import fr.hardel.asset_editor.client.compose.components.page.recipe.utils.paintSlotEdit
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SlotInteractionTest {

    private val planks = "minecraft:acacia_planks"
    private val stone = "minecraft:stone"

    @Test
    fun paintEmptySlotPlacesItem() {
        val edit = paintSlotEdit("4", planks, emptyMap())!!
        assertEquals(SlotEditKind.PAINT, edit.kind)
        assertEquals(mapOf("4" to listOf(planks)), edit.slots)
    }

    @Test
    fun paintPreservesOtherSlots() {
        val current = mapOf("0" to listOf(planks), "1" to listOf(planks))
        val edit = paintSlotEdit("5", stone, current)!!
        assertEquals(3, edit.slots.size)
        assertEquals(listOf(planks), edit.slots["0"])
        assertEquals(listOf(stone), edit.slots["5"])
    }

    @Test
    fun paintSameItemOnSameSlotIsNoOp() {
        val current = mapOf("3" to listOf(stone))
        assertNull(paintSlotEdit("3", stone, current))
    }

    @Test
    fun paintWithoutSelectedItemIsNoOp() {
        assertNull(paintSlotEdit("0", null, emptyMap()))
    }

    @Test
    fun eraseFilledSlotRemovesIt() {
        val current = mapOf("0" to listOf(planks), "3" to listOf(stone))
        val edit = eraseSlotEdit("3", current)!!
        assertEquals(SlotEditKind.ERASE, edit.kind)
        assertEquals(mapOf("0" to listOf(planks)), edit.slots)
    }

    @Test
    fun eraseEmptySlotIsNoOp() {
        assertNull(eraseSlotEdit("5", mapOf("0" to listOf(planks))))
    }

    @Test
    fun eraseLastSlotProducesEmptyEdit() {
        val edit = eraseSlotEdit("4", mapOf("4" to listOf(planks)))!!
        assertTrue(edit.slots.isEmpty())
    }

    @Test
    fun toServerSlotsConvertsKeysAndItems() {
        val edit = paintSlotEdit("7", planks, emptyMap())!!
        val server = edit.toServerSlots()
        assertEquals(setOf(7), server.keys)
        assertEquals(listOf("minecraft:acacia_planks"), server[7]?.map { it.toString() })
    }
}

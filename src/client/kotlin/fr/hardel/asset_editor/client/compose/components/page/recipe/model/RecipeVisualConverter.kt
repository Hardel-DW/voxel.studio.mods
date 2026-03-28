package fr.hardel.asset_editor.client.compose.components.page.recipe.model

import fr.hardel.asset_editor.client.compose.lib.data.RecipeTreeData
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.util.context.ContextMap
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay
import net.minecraft.world.item.crafting.display.SlotDisplay
import net.minecraft.world.item.crafting.display.SlotDisplayContext
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay
import net.minecraft.world.item.crafting.display.StonecutterRecipeDisplay

fun createDisplayContext(registries: HolderLookup.Provider): ContextMap =
    ContextMap.Builder()
        .withParameter(SlotDisplayContext.REGISTRIES, registries)
        .create(SlotDisplayContext.CONTEXT)

fun Recipe<*>.toVisualModel(serializerId: String, displayContext: ContextMap?): RecipeVisualModel {
    if (displayContext == null) return placeholderRecipeVisual(serializerId)

    val display = display().firstOrNull() ?: return placeholderRecipeVisual(serializerId)
    val fallbackResult = RecipeTreeData.getBlockByRecipeType(serializerId).blockId.toString()

    return when (display) {
        is ShapedCraftingRecipeDisplay -> buildIndexedSlotModel(
            serializerId, display.ingredients(), display.result(), displayContext, fallbackResult
        )
        is ShapelessCraftingRecipeDisplay -> buildIndexedSlotModel(
            serializerId, display.ingredients(), display.result(), displayContext, fallbackResult
        )
        is FurnaceRecipeDisplay -> RecipeVisualModel(
            type = serializerId,
            slots = mapOf("0" to resolveSlotOptions(display.ingredient(), displayContext)).filterValues { it.isNotEmpty() },
            resultItemId = resolveFirstItemId(display.result(), displayContext) ?: fallbackResult,
            resultCount = resolveResultCount(display.result(), displayContext)
        )
        is SmithingRecipeDisplay -> RecipeVisualModel(
            type = serializerId,
            slots = linkedMapOf(
                "0" to resolveSlotOptions(display.template(), displayContext),
                "1" to resolveSlotOptions(display.base(), displayContext),
                "2" to resolveSlotOptions(display.addition(), displayContext)
            ).filterValues { it.isNotEmpty() },
            resultItemId = resolveFirstItemId(display.result(), displayContext) ?: fallbackResult,
            resultCount = resolveResultCount(display.result(), displayContext)
        )
        is StonecutterRecipeDisplay -> RecipeVisualModel(
            type = serializerId,
            slots = mapOf("0" to resolveSlotOptions(display.input(), displayContext)).filterValues { it.isNotEmpty() },
            resultItemId = resolveFirstItemId(display.result(), displayContext) ?: fallbackResult,
            resultCount = resolveResultCount(display.result(), displayContext)
        )
        else -> placeholderRecipeVisual(serializerId)
    }
}

fun placeholderRecipeVisual(type: String): RecipeVisualModel {
    return when (RecipeTreeData.getBlockByRecipeType(type).templateKind) {
        RecipeTreeData.RecipeTemplateKind.SMITHING -> RecipeVisualModel(
            type = type,
            slots = linkedMapOf(
                "0" to listOf("minecraft:netherite_upgrade_smithing_template"),
                "1" to listOf("minecraft:diamond_sword"),
                "2" to listOf("minecraft:netherite_ingot")
            ),
            resultItemId = "minecraft:netherite_sword"
        )
        RecipeTreeData.RecipeTemplateKind.STONECUTTING -> RecipeVisualModel(
            type = type,
            slots = mapOf("0" to listOf("minecraft:stone")),
            resultItemId = "minecraft:stone_bricks"
        )
        RecipeTreeData.RecipeTemplateKind.SMELTING -> RecipeVisualModel(
            type = type,
            slots = mapOf("0" to listOf("minecraft:iron_ore")),
            resultItemId = "minecraft:iron_ingot"
        )
        RecipeTreeData.RecipeTemplateKind.CRAFTING -> RecipeVisualModel(
            type = type,
            slots = linkedMapOf(
                "0" to listOf("minecraft:oak_planks"),
                "1" to listOf("minecraft:oak_planks"),
                "3" to listOf("minecraft:stick"),
                "4" to listOf("minecraft:stick")
            ),
            resultItemId = "minecraft:wooden_sword"
        )
    }
}

private fun buildIndexedSlotModel(
    serializerId: String,
    ingredients: List<SlotDisplay>,
    result: SlotDisplay,
    displayContext: ContextMap,
    fallbackResult: String
): RecipeVisualModel = RecipeVisualModel(
    type = serializerId,
    slots = ingredients
        .mapIndexedNotNull { index, slot ->
            resolveSlotOptions(slot, displayContext)
                .takeIf { it.isNotEmpty() }
                ?.let { index.toString() to it }
        }
        .toMap(),
    resultItemId = resolveFirstItemId(result, displayContext) ?: fallbackResult,
    resultCount = resolveResultCount(result, displayContext)
)

private fun resolveSlotOptions(slot: SlotDisplay, displayContext: ContextMap): List<String> =
    slot.resolveForStacks(displayContext)
        .mapNotNull { stack -> BuiltInRegistries.ITEM.getKey(stack.item).toString() }
        .distinct()
        .take(16)

private fun resolveFirstItemId(slot: SlotDisplay, displayContext: ContextMap): String? =
    BuiltInRegistries.ITEM.getKey(slot.resolveForFirstStack(displayContext).item).toString()

private fun resolveResultCount(slot: SlotDisplay, displayContext: ContextMap): Int =
    slot.resolveForFirstStack(displayContext).count.takeIf { it > 0 } ?: 1

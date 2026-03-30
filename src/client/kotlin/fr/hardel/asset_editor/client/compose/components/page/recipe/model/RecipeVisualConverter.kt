package fr.hardel.asset_editor.client.compose.components.page.recipe.model

import fr.hardel.asset_editor.client.compose.lib.data.RecipeTreeData
import fr.hardel.asset_editor.workspace.action.recipe.adapter.RecipeAdapterRegistry
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
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
            resultCount = resolveResultCount(display.result(), displayContext),
            resultCountEditable = RecipeAdapterRegistry.supportsResultCount(this),
            resultCountMax = resolveResultMaxCount(display.result(), displayContext)
        )
        is SmithingRecipeDisplay -> RecipeVisualModel(
            type = serializerId,
            slots = linkedMapOf(
                "0" to resolveSlotOptions(display.template(), displayContext),
                "1" to resolveSlotOptions(display.base(), displayContext),
                "2" to resolveSlotOptions(display.addition(), displayContext)
            ).filterValues { it.isNotEmpty() },
            resultItemId = resolveFirstItemId(display.result(), displayContext) ?: fallbackResult,
            resultCount = resolveResultCount(display.result(), displayContext),
            resultCountEditable = RecipeAdapterRegistry.supportsResultCount(this),
            resultCountMax = resolveResultMaxCount(display.result(), displayContext)
        )
        is StonecutterRecipeDisplay -> RecipeVisualModel(
            type = serializerId,
            slots = mapOf("0" to resolveSlotOptions(display.input(), displayContext)).filterValues { it.isNotEmpty() },
            resultItemId = resolveFirstItemId(display.result(), displayContext) ?: fallbackResult,
            resultCount = resolveResultCount(display.result(), displayContext),
            resultCountEditable = RecipeAdapterRegistry.supportsResultCount(this),
            resultCountMax = resolveResultMaxCount(display.result(), displayContext)
        )
        else -> placeholderRecipeVisual(serializerId)
    }
}

fun placeholderRecipeVisual(type: String): RecipeVisualModel {
    val fallbackResult = RecipeTreeData.getBlockByRecipeType(type).blockId.toString()
    val resultCountEditable = Identifier.tryParse(type)?.let(RecipeAdapterRegistry::supportsResultCount) ?: false
    return RecipeVisualModel(
        type = type,
        slots = emptyMap(),
        resultItemId = fallbackResult,
        resultCountEditable = resultCountEditable
    )
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
    resultCount = resolveResultCount(result, displayContext),
    resultCountEditable = Identifier.tryParse(serializerId)?.let(RecipeAdapterRegistry::supportsResultCount) ?: false,
    resultCountMax = resolveResultMaxCount(result, displayContext)
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

private fun resolveResultMaxCount(slot: SlotDisplay, displayContext: ContextMap): Int =
    slot.resolveForFirstStack(displayContext).maxStackSize.takeIf { it > 0 } ?: 64

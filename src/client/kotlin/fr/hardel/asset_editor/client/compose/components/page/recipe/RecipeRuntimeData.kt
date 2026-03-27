package fr.hardel.asset_editor.client.compose.components.page.recipe

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.rememberRegistryEntries
import fr.hardel.asset_editor.client.compose.lib.data.RecipeTreeData
import fr.hardel.asset_editor.network.recipe.RecipeCatalogSyncPayload
import fr.hardel.asset_editor.store.ElementEntry
import net.minecraft.client.Minecraft
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
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

data class RecipeVisualModel(
    val type: String,
    val slots: Map<String, List<String>>,
    val resultItemId: String,
    val resultCount: Int = 1
)

data class RecipeRuntimeEntry(
    val id: Identifier,
    val type: String,
    val serializer: String,
    val visual: RecipeVisualModel
)

@Composable
fun rememberRecipeEntries(context: StudioContext): List<RecipeRuntimeEntry> {
    val workspaceEntries = rememberRegistryEntries(context, Registries.RECIPE)
    val recipeCatalog = context.sessionMemory().recipeCatalog()
    val connection = Minecraft.getInstance().connection
    val runtimeEntries = rememberRuntimeRecipeEntries(context)
    return remember(workspaceEntries, recipeCatalog, runtimeEntries, connection, context.sessionMemory().worldSessionKey()) {
        loadWorkspaceRecipeEntries(workspaceEntries, connection?.registryAccess())
            .ifEmpty { runtimeEntries }
            .ifEmpty { recipeCatalog.map(RecipeCatalogSyncPayload.Entry::toRecipeRuntimeEntry) }
    }
}

@Composable
fun rememberRecipeEntry(
    context: StudioContext,
    elementId: String?
): RecipeRuntimeEntry? {
    val entries = rememberRecipeEntries(context)
    return remember(entries, elementId) {
        val parsed = elementId?.let(Identifier::tryParse) ?: return@remember null
        entries.firstOrNull { it.id == parsed }
    }
}

@Composable
fun rememberRuntimeRecipeEntries(context: StudioContext): List<RecipeRuntimeEntry> {
    val minecraft = Minecraft.getInstance()
    val connection = minecraft.connection
    val integratedServer = minecraft.singleplayerServer
    return remember(connection, integratedServer, context.sessionMemory().worldSessionKey()) {
        loadRuntimeRecipeEntries(minecraft)
    }
}

@Composable
fun rememberRuntimeRecipeEntry(
    context: StudioContext,
    elementId: String?
): RecipeRuntimeEntry? {
    val entries = rememberRecipeEntries(context)
    return remember(entries, elementId) {
        val parsed = elementId?.let(Identifier::tryParse) ?: return@remember null
        entries.firstOrNull { it.id == parsed }
    }
}

private fun loadWorkspaceRecipeEntries(
    entries: List<ElementEntry<Recipe<*>>>,
    registries: HolderLookup.Provider?
): List<RecipeRuntimeEntry> {
    if (entries.isEmpty()) {
        return emptyList()
    }

    val displayContext = registries?.let(::createDisplayContext)
    return entries.mapNotNull { entry ->
        entry.toRecipeRuntimeEntry(displayContext)
    }.sortedBy { it.id.toString() }
}

private fun loadRuntimeRecipeEntries(minecraft: Minecraft): List<RecipeRuntimeEntry> {
    val integratedServer = minecraft.singleplayerServer
    if (integratedServer != null) {
        val displayContext = createDisplayContext(integratedServer.registryAccess())
        return integratedServer.recipeManager.getRecipes()
            .mapNotNull { holder ->
                ElementEntry(
                    holder.id().identifier(),
                    holder.value(),
                    emptySet(),
                    fr.hardel.asset_editor.store.CustomFields.EMPTY
                ).toRecipeRuntimeEntry(displayContext)
            }
            .sortedBy { it.id.toString() }
    }

    val registries = minecraft.connection?.registryAccess() ?: return emptyList()
    val registry = registries.lookup(Registries.RECIPE).orElse(null) ?: return emptyList()
    val displayContext = createDisplayContext(registries)

    return registry.listElements()
        .toList()
        .mapNotNull { holder ->
            ElementEntry(holder.key().identifier(), holder.value(), emptySet(), fr.hardel.asset_editor.store.CustomFields.EMPTY)
                .toRecipeRuntimeEntry(displayContext)
        }
        .toList()
        .sortedBy { it.id.toString() }
}

private fun createDisplayContext(registries: HolderLookup.Provider): ContextMap =
    ContextMap.Builder()
        .withParameter(SlotDisplayContext.REGISTRIES, registries)
        .create(SlotDisplayContext.CONTEXT)

private fun ElementEntry<Recipe<*>>.toRecipeRuntimeEntry(displayContext: ContextMap?): RecipeRuntimeEntry? {
    val recipe = data()
    val serializerId = BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.serializer) ?: return null
    val serializer = serializerId.toString()

    return RecipeRuntimeEntry(
        id = id(),
        type = serializer,
        serializer = serializer,
        visual = recipe.toVisualModel(serializer, displayContext)
    )
}

private fun RecipeCatalogSyncPayload.Entry.toRecipeRuntimeEntry(): RecipeRuntimeEntry =
    RecipeRuntimeEntry(
        id = id(),
        type = type(),
        serializer = type(),
        visual = placeholderRecipeVisual(type())
    )

private fun Recipe<*>.toVisualModel(
    serializerId: String,
    displayContext: ContextMap?
): RecipeVisualModel {
    if (displayContext == null) {
        return placeholderRecipeVisual(serializerId)
    }

    val display = display().firstOrNull() ?: return placeholderRecipeVisual(serializerId)
    return when (display) {
        is ShapedCraftingRecipeDisplay -> RecipeVisualModel(
            type = serializerId,
            slots = display.ingredients()
                .mapIndexedNotNull { index, slot ->
                    resolveSlotOptions(slot, displayContext)
                        .takeIf { it.isNotEmpty() }
                        ?.let { index.toString() to it }
                }
                .toMap(),
            resultItemId = resolveFirstItemId(display.result(), displayContext)
                ?: RecipeTreeData.getBlockByRecipeType(serializerId).blockId.toString(),
            resultCount = resolveResultCount(display.result(), displayContext)
        )

        is ShapelessCraftingRecipeDisplay -> RecipeVisualModel(
            type = serializerId,
            slots = display.ingredients()
                .mapIndexedNotNull { index, slot ->
                    resolveSlotOptions(slot, displayContext)
                        .takeIf { it.isNotEmpty() }
                        ?.let { index.toString() to it }
                }
                .toMap(),
            resultItemId = resolveFirstItemId(display.result(), displayContext)
                ?: RecipeTreeData.getBlockByRecipeType(serializerId).blockId.toString(),
            resultCount = resolveResultCount(display.result(), displayContext)
        )

        is FurnaceRecipeDisplay -> RecipeVisualModel(
            type = serializerId,
            slots = mapOf("0" to resolveSlotOptions(display.ingredient(), displayContext)).filterValues { it.isNotEmpty() },
            resultItemId = resolveFirstItemId(display.result(), displayContext)
                ?: RecipeTreeData.getBlockByRecipeType(serializerId).blockId.toString(),
            resultCount = resolveResultCount(display.result(), displayContext)
        )

        is SmithingRecipeDisplay -> RecipeVisualModel(
            type = serializerId,
            slots = linkedMapOf(
                "0" to resolveSlotOptions(display.template(), displayContext),
                "1" to resolveSlotOptions(display.base(), displayContext),
                "2" to resolveSlotOptions(display.addition(), displayContext)
            ).filterValues { it.isNotEmpty() },
            resultItemId = resolveFirstItemId(display.result(), displayContext)
                ?: RecipeTreeData.getBlockByRecipeType(serializerId).blockId.toString(),
            resultCount = resolveResultCount(display.result(), displayContext)
        )

        is StonecutterRecipeDisplay -> RecipeVisualModel(
            type = serializerId,
            slots = mapOf("0" to resolveSlotOptions(display.input(), displayContext)).filterValues { it.isNotEmpty() },
            resultItemId = resolveFirstItemId(display.result(), displayContext)
                ?: RecipeTreeData.getBlockByRecipeType(serializerId).blockId.toString(),
            resultCount = resolveResultCount(display.result(), displayContext)
        )

        else -> placeholderRecipeVisual(serializerId)
    }
}

fun placeholderRecipeVisual(type: String): RecipeVisualModel {
    return when (RecipeTreeData.getBlockByRecipeType(type).blockId.toString()) {
        "minecraft:smithing_table" -> RecipeVisualModel(
            type = type,
            slots = linkedMapOf(
                "0" to listOf("minecraft:netherite_upgrade_smithing_template"),
                "1" to listOf("minecraft:diamond_sword"),
                "2" to listOf("minecraft:netherite_ingot")
            ),
            resultItemId = "minecraft:netherite_sword"
        )

        "minecraft:stonecutter" -> RecipeVisualModel(
            type = type,
            slots = mapOf("0" to listOf("minecraft:stone")),
            resultItemId = "minecraft:stone_bricks"
        )

        "minecraft:furnace",
        "minecraft:blast_furnace",
        "minecraft:smoker",
        "minecraft:campfire" -> RecipeVisualModel(
            type = type,
            slots = mapOf("0" to listOf("minecraft:iron_ore")),
            resultItemId = "minecraft:iron_ingot"
        )

        else -> RecipeVisualModel(
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

private fun resolveSlotOptions(
    slot: SlotDisplay,
    displayContext: ContextMap
): List<String> =
    slot.resolveForStacks(displayContext)
        .mapNotNull { stack -> BuiltInRegistries.ITEM.getKey(stack.item).toString() }
        .distinct()
        .take(16)

private fun resolveFirstItemId(
    slot: SlotDisplay,
    displayContext: ContextMap
): String? =
    BuiltInRegistries.ITEM.getKey(slot.resolveForFirstStack(displayContext).item).toString()

private fun resolveResultCount(
    slot: SlotDisplay,
    displayContext: ContextMap
): Int =
    slot.resolveForFirstStack(displayContext).count.takeIf { it > 0 } ?: 1

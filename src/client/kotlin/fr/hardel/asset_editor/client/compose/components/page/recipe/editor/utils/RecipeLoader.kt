package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils

import fr.hardel.asset_editor.network.recipe.RecipeCatalogEntry
import fr.hardel.asset_editor.workspace.flush.ElementEntry
import fr.hardel.asset_editor.workspace.action.recipe.adapter.RecipeAdapterRegistry
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.util.context.ContextMap
import net.minecraft.world.item.crafting.Recipe

fun loadWorkspaceRecipeEntries(
    entries: List<ElementEntry<Recipe<*>>>,
    registries: HolderLookup.Provider?
): List<RecipeRuntimeEntry> {
    if (entries.isEmpty()) return emptyList()

    val displayContext = registries?.let(::createDisplayContext)
    return entries.mapNotNull { it.toRuntimeEntry(displayContext) }
        .sortedBy { it.id.toString() }
}

fun RecipeCatalogEntry.toRuntimeEntry(): RecipeRuntimeEntry {
    val resultCountEditable = Identifier.tryParse(type())?.let(RecipeAdapterRegistry::supportsResultCount) ?: false
    val resultCountMax = Identifier.tryParse(resultItemId())
        ?.let(BuiltInRegistries.ITEM::getOptional)
        ?.map { it.defaultInstance.maxStackSize }
        ?.orElse(64)
        ?: 64
    return RecipeRuntimeEntry(
        id = id(),
        type = type(),
        serializer = type(),
        visual = RecipeVisualModel(
            type = type(),
            slots = slots(),
            resultItemId = resultItemId(),
            resultCount = resultCount(),
            resultCountEditable = resultCountEditable,
            resultCountMax = resultCountMax
        )
    )
}

private fun ElementEntry<Recipe<*>>.toRuntimeEntry(displayContext: ContextMap?): RecipeRuntimeEntry? {
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

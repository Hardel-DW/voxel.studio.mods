package fr.hardel.asset_editor.client.compose.components.page.recipe.model

import fr.hardel.asset_editor.network.recipe.RecipeCatalogSyncPayload
import fr.hardel.asset_editor.store.CustomFields
import fr.hardel.asset_editor.store.ElementEntry
import net.minecraft.client.Minecraft
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
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

fun loadRuntimeRecipeEntries(minecraft: Minecraft): List<RecipeRuntimeEntry> {
    val integratedServer = minecraft.singleplayerServer
    if (integratedServer != null) {
        val displayContext = createDisplayContext(integratedServer.registryAccess())
        return integratedServer.recipeManager.getRecipes()
            .mapNotNull { holder ->
                ElementEntry(holder.id().identifier(), holder.value(), emptySet(), CustomFields.EMPTY)
                    .toRuntimeEntry(displayContext)
            }
            .sortedBy { it.id.toString() }
    }

    val registries = minecraft.connection?.registryAccess() ?: return emptyList()
    val registry = registries.lookup(Registries.RECIPE).orElse(null) ?: return emptyList()
    val displayContext = createDisplayContext(registries)

    return registry.listElements()
        .toList()
        .mapNotNull { holder ->
            ElementEntry(holder.key().identifier(), holder.value(), emptySet(), CustomFields.EMPTY)
                .toRuntimeEntry(displayContext)
        }
        .sortedBy { it.id.toString() }
}

fun RecipeCatalogSyncPayload.Entry.toRuntimeEntry(): RecipeRuntimeEntry =
    RecipeRuntimeEntry(
        id = id(),
        type = type(),
        serializer = type(),
        visual = placeholderRecipeVisual(type())
    )

private fun ElementEntry<Recipe<*>>.toRuntimeEntry(displayContext: net.minecraft.util.context.ContextMap?): RecipeRuntimeEntry? {
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

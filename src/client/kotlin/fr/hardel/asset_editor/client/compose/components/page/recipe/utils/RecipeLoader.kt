package fr.hardel.asset_editor.client.compose.components.page.recipe.utils

import fr.hardel.asset_editor.network.recipe.RecipeCatalogEntry
import fr.hardel.asset_editor.workspace.flush.ElementEntry
import fr.hardel.asset_editor.workspace.action.recipe.adapter.RecipeAdapterRegistry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.SmithingTrimRecipe

fun loadWorkspaceRecipeEntries(
    entries: List<ElementEntry<Recipe<*>>>
): List<RecipeRuntimeEntry> {
    if (entries.isEmpty()) return emptyList()
    return entries.mapNotNull { it.toRuntimeEntry() }
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

private fun ElementEntry<Recipe<*>>.toRuntimeEntry(): RecipeRuntimeEntry? {
    val recipe = data()
    val serializerId = BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.serializer) ?: return null
    val serializer = serializerId.toString()

    if (RecipeAdapterRegistry.isUnsupported(recipe)) {
        return RecipeRuntimeEntry(
            id = id(),
            type = serializer,
            serializer = serializer,
            visual = placeholderRecipeVisual(serializer)
        )
    }

    val adapter = RecipeAdapterRegistry.get(recipe)
    val ingredients = adapter.extractIngredients(recipe)
    val result = adapter.extractResult(recipe)

    val slots = ingredients.mapIndexedNotNull { index, opt ->
        val ingredient = opt.orElse(null) ?: return@mapIndexedNotNull null
        val items = resolveIngredientItems(ingredient)
        if (items.isNotEmpty()) index.toString() to items else null
    }.toMap()

    val resultItemId = when {
        !result.isEmpty -> BuiltInRegistries.ITEM.getKey(result.item).toString()
        recipe is SmithingTrimRecipe -> firstIngredientItem(recipe.baseIngredient())
        else -> ""
    }

    return RecipeRuntimeEntry(
        id = id(),
        type = serializer,
        serializer = serializer,
        visual = RecipeVisualModel(
            type = serializer,
            slots = slots,
            resultItemId = resultItemId,
            resultCount = result.count.coerceAtLeast(1),
            resultCountEditable = adapter.supportsResultCount(),
            resultCountMax = result.maxStackSize.coerceAtLeast(1)
        )
    )
}

private fun resolveIngredientItems(ingredient: Ingredient): List<String> =
    ingredientItemIds(ingredient)
        .distinct()
        .limit(RecipeCatalogEntry.MAX_INGREDIENT_OPTIONS.toLong())
        .toList()

private fun firstIngredientItem(ingredient: Ingredient): String =
    ingredientItemIds(ingredient).findFirst().orElse("")

private fun ingredientItemIds(ingredient: Ingredient) =
    ingredient.values.stream()
        .map { holder -> holder.unwrapKey().map(ResourceKey<*>::identifier).orElse(null) }
        .filter { it != null }
        .map { it.toString() }

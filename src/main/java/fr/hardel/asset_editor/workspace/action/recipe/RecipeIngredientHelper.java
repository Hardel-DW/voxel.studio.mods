package fr.hardel.asset_editor.workspace.action.recipe;

import fr.hardel.asset_editor.workspace.action.recipe.adapter.RecipeAdapterRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import org.jspecify.annotations.Nullable;

import java.util.*;

public final class RecipeIngredientHelper {

    private final Provider registries;

    public RecipeIngredientHelper(Provider registries) {
        this.registries = registries;
    }

    public Holder<Item> resolveItem(Identifier id) {
        var lookup = registries.lookupOrThrow(Registries.ITEM);
        return lookup.getOrThrow(ResourceKey.create(Registries.ITEM, id));
    }

    public Ingredient toIngredient(List<Identifier> itemIds) {
        List<Holder<Item>> holders = itemIds.stream().map(this::resolveItem).toList();
        return Ingredient.of(HolderSet.direct(holders));
    }

    public Ingredient merge(Ingredient existing, List<Identifier> newItemIds) {
        Set<Identifier> seen = new LinkedHashSet<>();
        List<Holder<Item>> merged = new ArrayList<>();

        existing.values.stream().forEach(holder -> {
            Identifier id = holder.unwrapKey()
                .map(ResourceKey::identifier)
                .orElse(null);

            if (id != null && seen.add(id)) {
                merged.add(holder);
            }
        });

        for (Identifier newId : newItemIds) {
            if (seen.add(newId)) {
                merged.add(resolveItem(newId));
            }
        }

        return Ingredient.of(HolderSet.direct(merged));
    }

    public @Nullable Ingredient remove(Ingredient existing, List<Identifier> toRemove) {
        Set<Identifier> removeSet = new HashSet<>(toRemove);
        List<Holder<Item>> remaining = existing.values.stream()
            .filter(holder -> holder.unwrapKey()
                .map(key -> !removeSet.contains(key.identifier()))
                .orElse(true))
            .toList();

        if (remaining.isEmpty()) {
            return null;
        }
        return Ingredient.of(HolderSet.direct(remaining));
    }

    public @Nullable Ingredient replace(Ingredient existing, Identifier from, Identifier to) {
        Holder<Item> toHolder = resolveItem(to);
        Set<Identifier> seen = new LinkedHashSet<>();
        List<Holder<Item>> replaced = new ArrayList<>();

        existing.values.stream().forEach(holder -> {
            Identifier id = holder.unwrapKey()
                .map(ResourceKey::identifier)
                .orElse(null);
            if (id == null)
                return;

            Holder<Item> target = id.equals(from) ? toHolder : holder;
            Identifier targetId = target.unwrapKey()
                .map(ResourceKey::identifier)
                .orElse(id);

            if (seen.add(targetId)) {
                replaced.add(target);
            }
        });

        if (replaced.isEmpty()) {
            return null;
        }
        return Ingredient.of(HolderSet.direct(replaced));
    }

    public List<Optional<Ingredient>> extractIngredients(Recipe<?> recipe) {
        return RecipeAdapterRegistry.extractIngredients(recipe);
    }

    public ItemStack extractResult(Recipe<?> recipe) {
        return RecipeAdapterRegistry.extractResult(recipe);
    }

    public Recipe<?> rebuild(Recipe<?> recipe, List<Optional<Ingredient>> ingredients) {
        return RecipeAdapterRegistry.rebuild(recipe, ingredients);
    }

    public @Nullable Recipe<?> convertRecipeType(Recipe<?> recipe, Identifier targetSerializer, boolean preserveIngredients) {
        return RecipeAdapterRegistry.convert(recipe, targetSerializer, preserveIngredients);
    }

    public @Nullable Recipe<?> setResultCount(Recipe<?> recipe, int count) {
        return RecipeAdapterRegistry.setResultCount(recipe, count);
    }

    public @Nullable Recipe<?> setResultItem(Recipe<?> recipe, Identifier itemId) {
        return RecipeAdapterRegistry.setResultItem(recipe, resolveItem(itemId));
    }

    public @Nullable Recipe<?> setGroup(Recipe<?> recipe, String group) {
        return RecipeAdapterRegistry.setGroup(recipe, group);
    }

    public @Nullable Recipe<?> setCraftingCategory(Recipe<?> recipe, CraftingBookCategory category) {
        return RecipeAdapterRegistry.setCraftingCategory(recipe, category);
    }

    public @Nullable Recipe<?> setCookingCategory(Recipe<?> recipe, CookingBookCategory category) {
        return RecipeAdapterRegistry.setCookingCategory(recipe, category);
    }

    public @Nullable Recipe<?> setCookingExperience(Recipe<?> recipe, float experience) {
        return RecipeAdapterRegistry.setCookingExperience(recipe, experience);
    }

    public @Nullable Recipe<?> setCookingTime(Recipe<?> recipe, int cookingTime) {
        return RecipeAdapterRegistry.setCookingTime(recipe, cookingTime);
    }

    public @Nullable Recipe<?> setShowNotification(Recipe<?> recipe, boolean value) {
        return RecipeAdapterRegistry.setShowNotification(recipe, value);
    }
}

package fr.hardel.asset_editor.workspace.action.recipe.adapter;

import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class RecipeAdapterRegistry {

    private static final Map<Class<?>, RecipeAdapter<?>> BY_CLASS = new ConcurrentHashMap<>();
    private static final Map<Identifier, RecipeAdapter<?>> BY_SERIALIZER = new ConcurrentHashMap<>();

    public static void register(RecipeAdapter<?> adapter) {
        Class<?> type = adapter.recipeType();
        if (BY_CLASS.containsKey(type)) {
            throw new IllegalStateException("Adapter already registered for " + type.getSimpleName());
        }
        BY_CLASS.put(type, adapter);
        BY_SERIALIZER.put(adapter.serializerId(), adapter);
    }

    public static RecipeAdapter<?> get(Recipe<?> recipe) {
        Class<?> type = recipe.getClass();
        while (type != null && Recipe.class.isAssignableFrom(type)) {
            RecipeAdapter<?> adapter = BY_CLASS.get(type);
            if (adapter != null) {
                return adapter;
            }
            type = type.getSuperclass();
        }
        throw new UnsupportedOperationException("No adapter registered for recipe type: " + recipe.getClass().getSimpleName());
    }

    public static @Nullable RecipeAdapter<?> getBySerializer(Identifier serializerId) {
        return BY_SERIALIZER.get(serializerId);
    }

    public static boolean isUnsupported(Recipe<?> recipe) {
        Class<?> type = recipe.getClass();
        while (type != null && Recipe.class.isAssignableFrom(type)) {
            if (BY_CLASS.containsKey(type)) return false;
            type = type.getSuperclass();
        }
        return true;
    }

    public static List<Optional<Ingredient>> extractIngredients(Recipe<?> recipe) {
        return get(recipe).extractIngredients(recipe);
    }

    public static ItemStack extractResult(Recipe<?> recipe) {
        return get(recipe).extractResult(recipe);
    }

    public static Recipe<?> rebuild(Recipe<?> recipe, List<Optional<Ingredient>> ingredients) {
        return get(recipe).rebuild(recipe, ingredients);
    }

    public static @Nullable Recipe<?> convert(Recipe<?> source, Identifier targetSerializer, boolean preserveIngredients) {
        RecipeAdapter<?> targetAdapter = getBySerializer(targetSerializer);
        if (targetAdapter == null || isUnsupported(source)) return null;

        RecipeAdapter<?> sourceAdapter = get(source);
        List<Optional<Ingredient>> ingredients = preserveIngredients
            ? sourceAdapter.extractIngredients(source)
            : List.of();
        ItemStack result = sourceAdapter.extractResult(source);

        return targetAdapter.buildFromGeneric(ingredients, result);
    }

    public static @Nullable Recipe<?> setResultCount(Recipe<?> recipe, int count) {
        return get(recipe).setResultCount(recipe, count);
    }

    public static @Nullable Recipe<?> setResultItem(Recipe<?> recipe, Holder<Item> item) {
        return get(recipe).setResultItem(recipe, item);
    }

    public static @Nullable Recipe<?> setGroup(Recipe<?> recipe, String group) {
        return get(recipe).setGroup(recipe, group);
    }

    public static @Nullable Recipe<?> setCraftingCategory(Recipe<?> recipe, CraftingBookCategory category) {
        return get(recipe).setCraftingCategory(recipe, category);
    }

    public static @Nullable Recipe<?> setCookingCategory(Recipe<?> recipe, CookingBookCategory category) {
        return get(recipe).setCookingCategory(recipe, category);
    }

    public static @Nullable Recipe<?> setCookingExperience(Recipe<?> recipe, float experience) {
        return get(recipe).setCookingExperience(recipe, experience);
    }

    public static @Nullable Recipe<?> setCookingTime(Recipe<?> recipe, int cookingTime) {
        return get(recipe).setCookingTime(recipe, cookingTime);
    }

    public static @Nullable Recipe<?> setShowNotification(Recipe<?> recipe, boolean value) {
        return get(recipe).setShowNotification(recipe, value);
    }

    public static boolean supportsResultCount(Recipe<?> recipe) {
        return get(recipe).supportsResultCount();
    }

    public static boolean supportsResultCount(Identifier serializerId) {
        RecipeAdapter<?> adapter = getBySerializer(serializerId);
        return adapter != null && adapter.supportsResultCount();
    }

    public static boolean supportsResultItem(Recipe<?> recipe) {
        return get(recipe).supportsResultItem();
    }

    private RecipeAdapterRegistry() {
    }
}

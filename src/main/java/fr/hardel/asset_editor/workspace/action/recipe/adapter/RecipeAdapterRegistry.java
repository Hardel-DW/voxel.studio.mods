package fr.hardel.asset_editor.workspace.action.recipe.adapter;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.Item;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
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
            if (BY_CLASS.containsKey(type))
                return false;
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
        if (targetAdapter == null || isUnsupported(source))
            return null;

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

    public static @Nullable Recipe<?> setResultComponents(Recipe<?> recipe, DataComponentPatch patch) {
        return get(recipe).setResultComponents(recipe, patch);
    }

    public static boolean supportsResultComponents(Recipe<?> recipe) {
        if (isUnsupported(recipe)) return false;
        return get(recipe).supportsResultComponents();
    }

    public static Map<String, Object> extractProperties(Recipe<?> recipe) {
        if (isUnsupported(recipe))
            return Collections.emptyMap();
        return get(recipe).extractPropertiesFrom(recipe);
    }

    public static @Nullable Recipe<?> setProperty(Recipe<?> recipe, String key, Object value) {
        if (isUnsupported(recipe))
            return null;
        return get(recipe).setProperty(recipe, key, value);
    }

    private RecipeAdapterRegistry() {}
}

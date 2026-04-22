package fr.hardel.asset_editor.workspace.action.recipe.adapter;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.Item;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.TransmuteResult;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class RecipeAdapter<T extends Recipe<?>> {

    public static final String GROUP = "group";
    public static final String SHOW_NOTIFICATION = "show_notification";

    private final Class<T> recipeType;
    private final Identifier serializerId;

    protected RecipeAdapter(Class<T> recipeType, Identifier serializerId) {
        this.recipeType = recipeType;
        this.serializerId = serializerId;
    }

    public Class<T> recipeType() {
        return recipeType;
    }

    public Identifier serializerId() {
        return serializerId;
    }

    public final List<Optional<Ingredient>> extractIngredients(Recipe<?> recipe) {
        return doExtractIngredients(recipeType.cast(recipe));
    }

    public final ItemStack extractResult(Recipe<?> recipe) {
        return doExtractResult(recipeType.cast(recipe));
    }

    public final Recipe<?> rebuild(Recipe<?> recipe, List<Optional<Ingredient>> ingredients) {
        return doRebuild(recipeType.cast(recipe), ingredients);
    }

    public final @Nullable Recipe<?> setResultCount(Recipe<?> recipe, int count) {
        return doSetResultCount(recipeType.cast(recipe), count);
    }

    public final @Nullable Recipe<?> setResultItem(Recipe<?> recipe, Holder<Item> item) {
        return doSetResultItem(recipeType.cast(recipe), item);
    }

    public final @Nullable Recipe<?> setResultComponents(Recipe<?> recipe, DataComponentPatch patch) {
        return doSetResultComponents(recipeType.cast(recipe), patch);
    }

    public boolean supportsResultCount() {
        return false;
    }

    public boolean supportsResultItem() {
        return true;
    }

    public boolean supportsResultComponents() {
        return supportsResultItem();
    }

    public abstract @Nullable Recipe<?> buildFromGeneric(List<Optional<Ingredient>> ingredients, ItemStack result);

    protected abstract List<Optional<Ingredient>> doExtractIngredients(T recipe);

    protected abstract ItemStack doExtractResult(T recipe);

    protected abstract T doRebuild(T original, List<Optional<Ingredient>> ingredients);

    protected @Nullable T doSetResultCount(T recipe, int count) {
        return null;
    }

    protected @Nullable T doSetResultItem(T recipe, Holder<Item> item) {
        return null;
    }

    protected @Nullable T doSetResultComponents(T recipe, DataComponentPatch patch) {
        return null;
    }

    public final Map<String, Object> extractPropertiesFrom(Recipe<?> recipe) {
        return extractProperties(recipeType.cast(recipe));
    }

    protected Map<String, Object> extractProperties(T recipe) {
        var props = new LinkedHashMap<String, Object>();
        props.put(GROUP, recipe.group());
        props.put(SHOW_NOTIFICATION, recipe.showNotification());
        return props;
    }

    public final @Nullable Recipe<?> setProperty(Recipe<?> recipe, String key, Object value) {
        return doSetProperty(recipeType.cast(recipe), key, value);
    }

    protected @Nullable T doSetProperty(T recipe, String key, Object value) {
        return null;
    }

    protected static ItemStack replaceResultStack(Holder<Item> item, int count, DataComponentPatch components) {
        int clampedCount = clampResultCount(item, count);
        ItemStack candidate = new ItemStack(item, clampedCount, components);
        return ItemStack.validateStrict(candidate).result().isPresent() ? candidate : new ItemStack(item, clampedCount);
    }

    protected static @Nullable ItemStack validatedResultStack(Holder<Item> item, int count, DataComponentPatch components) {
        ItemStack candidate = new ItemStack(item, count, components);
        return ItemStack.validateStrict(candidate).result().isPresent() ? candidate : null;
    }

    protected static TransmuteResult replaceTransmuteResult(Holder<Item> item, int count, DataComponentPatch components) {
        int clampedCount = clampResultCount(item, count);
        ItemStack candidate = new ItemStack(item, clampedCount, components);
        if (ItemStack.validateStrict(candidate).result().isPresent()) {
            return new TransmuteResult(item, clampedCount, components);
        }
        return new TransmuteResult(item, clampedCount, DataComponentPatch.EMPTY);
    }

    private static int clampResultCount(Holder<Item> item, int count) {
        int maxCount = Math.min(new ItemStack(item).getMaxStackSize(), 99);
        return Math.max(1, Math.min(count, maxCount));
    }
}

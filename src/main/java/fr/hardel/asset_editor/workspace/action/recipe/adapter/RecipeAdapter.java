package fr.hardel.asset_editor.workspace.action.recipe.adapter;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.Item;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.TransmuteResult;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public abstract class RecipeAdapter<T extends Recipe<?>> {

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

    public final @Nullable Recipe<?> setGroup(Recipe<?> recipe, String group) {
        return doSetGroup(recipeType.cast(recipe), group);
    }

    public final @Nullable Recipe<?> setCraftingCategory(Recipe<?> recipe, CraftingBookCategory category) {
        return doSetCraftingCategory(recipeType.cast(recipe), category);
    }

    public final @Nullable Recipe<?> setCookingCategory(Recipe<?> recipe, CookingBookCategory category) {
        return doSetCookingCategory(recipeType.cast(recipe), category);
    }

    public final @Nullable Recipe<?> setCookingExperience(Recipe<?> recipe, float experience) {
        return doSetCookingExperience(recipeType.cast(recipe), experience);
    }

    public final @Nullable Recipe<?> setCookingTime(Recipe<?> recipe, int cookingTime) {
        return doSetCookingTime(recipeType.cast(recipe), cookingTime);
    }

    public final @Nullable Recipe<?> setShowNotification(Recipe<?> recipe, boolean value) {
        return doSetShowNotification(recipeType.cast(recipe), value);
    }

    public boolean supportsResultCount() {
        return false;
    }

    public boolean supportsResultItem() {
        return true;
    }

    public boolean supportsGroup() {
        return false;
    }

    public boolean supportsCraftingCategory() {
        return false;
    }

    public boolean supportsCookingCategory() {
        return false;
    }

    public boolean supportsCookingExperience() {
        return false;
    }

    public boolean supportsCookingTime() {
        return false;
    }

    public boolean supportsShowNotification() {
        return false;
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

    protected @Nullable T doSetGroup(T recipe, String group) {
        return null;
    }

    protected @Nullable T doSetCraftingCategory(T recipe, CraftingBookCategory category) {
        return null;
    }

    protected @Nullable T doSetCookingCategory(T recipe, CookingBookCategory category) {
        return null;
    }

    protected @Nullable T doSetCookingExperience(T recipe, float experience) {
        return null;
    }

    protected @Nullable T doSetCookingTime(T recipe, int cookingTime) {
        return null;
    }

    protected @Nullable T doSetShowNotification(T recipe, boolean value) {
        return null;
    }

    protected static ItemStack replaceResultStack(Holder<Item> item, int count, DataComponentPatch components) {
        int clampedCount = clampResultCount(item, count);
        ItemStack candidate = new ItemStack(item, clampedCount, components);
        return ItemStack.validateStrict(candidate).result().isPresent() ? candidate : new ItemStack(item, clampedCount);
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

package fr.hardel.asset_editor.workspace.action.recipe.adapter;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
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

    public boolean supportsResultCount() {
        return false;
    }

    public boolean supportsConversionTarget() {
        return false;
    }

    public @Nullable Recipe<?> convertFrom(Recipe<?> source, boolean preserveIngredients) {
        return null;
    }

    protected abstract List<Optional<Ingredient>> doExtractIngredients(T recipe);
    protected abstract ItemStack doExtractResult(T recipe);
    protected abstract T doRebuild(T original, List<Optional<Ingredient>> ingredients);

    protected @Nullable T doSetResultCount(T recipe, int count) {
        return null;
    }
}

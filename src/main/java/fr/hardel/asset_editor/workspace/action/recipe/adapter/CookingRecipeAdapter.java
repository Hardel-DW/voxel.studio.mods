package fr.hardel.asset_editor.workspace.action.recipe.adapter;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CookingRecipeAdapter<T extends AbstractCookingRecipe> extends RecipeAdapter<T> {

    private final AbstractCookingRecipe.Factory<T> factory;
    private final int defaultCookingTime;

    public CookingRecipeAdapter(Class<T> recipeType, Identifier serializerId, AbstractCookingRecipe.Factory<T> factory, int defaultCookingTime) {
        super(recipeType, serializerId);
        this.factory = factory;
        this.defaultCookingTime = defaultCookingTime;
    }

    @Override
    protected List<Optional<Ingredient>> doExtractIngredients(T recipe) {
        return new ArrayList<>(List.of(Optional.of(recipe.input())));
    }

    @Override
    protected ItemStack doExtractResult(T recipe) {
        return recipe.result().copy();
    }

    @Override
    protected T doRebuild(T original, List<Optional<Ingredient>> ingredients) {
        Ingredient input = ingredients.isEmpty()
            ? original.input()
            : ingredients.getFirst().orElse(original.input());

        return factory.create(
            original.group(), original.category(), input,
            original.result().copy(), original.experience(), original.cookingTime()
        );
    }

    @Override
    protected T doSetResultCount(T recipe, int count) {
        return factory.create(
            recipe.group(), recipe.category(), recipe.input(),
            recipe.result().copyWithCount(count), recipe.experience(), recipe.cookingTime()
        );
    }

    @Override
    public boolean supportsResultCount() {
        return true;
    }

    @Override
    public boolean supportsConversionTarget() {
        return true;
    }

    @Override
    public @Nullable Recipe<?> convertFrom(Recipe<?> source, boolean preserveIngredients) {
        if (!(source instanceof AbstractCookingRecipe cooking)) return null;

        Ingredient input = preserveIngredients ? cooking.input() : Ingredient.of(Items.STONE);
        ItemStack result = RecipeAdapterRegistry.extractResult(source);

        return factory.create(
            source.group(), CookingBookCategory.MISC, input,
            result, cooking.experience(), defaultCookingTime
        );
    }
}

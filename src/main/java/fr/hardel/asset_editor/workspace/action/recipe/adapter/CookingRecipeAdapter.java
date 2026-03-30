package fr.hardel.asset_editor.workspace.action.recipe.adapter;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;

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
    public Recipe<?> buildFromGeneric(List<Optional<Ingredient>> ingredients, ItemStack result) {
        Ingredient input = ingredients.stream()
            .flatMap(Optional::stream)
            .findFirst()
            .orElse(Ingredient.of(Items.STONE));

        return factory.create("", CookingBookCategory.MISC, input, result, 0f, defaultCookingTime);
    }
}

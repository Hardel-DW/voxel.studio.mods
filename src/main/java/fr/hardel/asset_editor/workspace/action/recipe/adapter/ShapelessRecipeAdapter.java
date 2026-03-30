package fr.hardel.asset_editor.workspace.action.recipe.adapter;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ShapelessRecipeAdapter extends RecipeAdapter<ShapelessRecipe> {

    private static final Identifier SERIALIZER_ID = Identifier.withDefaultNamespace("crafting_shapeless");

    public ShapelessRecipeAdapter() {
        super(ShapelessRecipe.class, SERIALIZER_ID);
    }

    @Override
    protected List<Optional<Ingredient>> doExtractIngredients(ShapelessRecipe recipe) {
        return recipe.ingredients.stream()
            .map(Optional::of)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    protected ItemStack doExtractResult(ShapelessRecipe recipe) {
        return recipe.result.copy();
    }

    @Override
    protected ShapelessRecipe doRebuild(ShapelessRecipe original, List<Optional<Ingredient>> ingredients) {
        List<Ingredient> compacted = ingredients.stream()
            .flatMap(Optional::stream)
            .toList();
        return new ShapelessRecipe(original.group(), original.category(), original.result.copy(), compacted);
    }

    @Override
    protected ShapelessRecipe doSetResultCount(ShapelessRecipe recipe, int count) {
        return new ShapelessRecipe(recipe.group(), recipe.category(), recipe.result.copyWithCount(count), recipe.ingredients);
    }

    @Override
    public boolean supportsResultCount() {
        return true;
    }

    @Override
    public Recipe<?> buildFromGeneric(List<Optional<Ingredient>> ingredients, ItemStack result) {
        List<Ingredient> compacted = ingredients.stream()
            .flatMap(Optional::stream)
            .limit(9)
            .toList();

        return new ShapelessRecipe("", CraftingBookCategory.MISC, result, compacted);
    }
}

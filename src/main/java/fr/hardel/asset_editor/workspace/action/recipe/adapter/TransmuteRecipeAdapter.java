package fr.hardel.asset_editor.workspace.action.recipe.adapter;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.TransmuteRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class TransmuteRecipeAdapter extends RecipeAdapter<TransmuteRecipe> {

    private static final Identifier SERIALIZER_ID = Identifier.withDefaultNamespace("crafting_transmute");

    public TransmuteRecipeAdapter() {
        super(TransmuteRecipe.class, SERIALIZER_ID);
    }

    @Override
    protected List<Optional<Ingredient>> doExtractIngredients(TransmuteRecipe recipe) {
        return new ArrayList<>(List.of(
            Optional.of(recipe.input),
            Optional.of(recipe.material)
        ));
    }

    @Override
    protected ItemStack doExtractResult(TransmuteRecipe recipe) {
        return new ItemStack(recipe.result.item(), recipe.result.count(), recipe.result.components());
    }

    @Override
    protected TransmuteRecipe doRebuild(TransmuteRecipe original, List<Optional<Ingredient>> ingredients) {
        Ingredient input = ingredients.size() > 0 ? ingredients.get(0).orElse(original.input) : original.input;
        Ingredient material = ingredients.size() > 1 ? ingredients.get(1).orElse(original.material) : original.material;
        return new TransmuteRecipe(original.group(), original.category(), input, material, original.result);
    }
}

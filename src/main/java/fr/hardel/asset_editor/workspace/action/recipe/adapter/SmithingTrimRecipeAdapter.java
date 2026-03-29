package fr.hardel.asset_editor.workspace.action.recipe.adapter;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SmithingTrimRecipeAdapter extends RecipeAdapter<SmithingTrimRecipe> {

    private static final Identifier SERIALIZER_ID = Identifier.withDefaultNamespace("smithing_trim");

    public SmithingTrimRecipeAdapter() {
        super(SmithingTrimRecipe.class, SERIALIZER_ID);
    }

    @Override
    protected List<Optional<Ingredient>> doExtractIngredients(SmithingTrimRecipe recipe) {
        return new ArrayList<>(List.of(
            recipe.templateIngredient(),
            Optional.of(recipe.baseIngredient()),
            recipe.additionIngredient()
        ));
    }

    @Override
    protected ItemStack doExtractResult(SmithingTrimRecipe recipe) {
        return ItemStack.EMPTY;
    }

    @Override
    protected SmithingTrimRecipe doRebuild(SmithingTrimRecipe original, List<Optional<Ingredient>> ingredients) {
        Ingredient template = ingredients.size() > 0
            ? ingredients.get(0).orElse(original.templateIngredient().orElseThrow())
            : original.templateIngredient().orElseThrow();

        Ingredient base = ingredients.size() > 1
            ? ingredients.get(1).orElse(original.baseIngredient())
            : original.baseIngredient();

        Ingredient addition = ingredients.size() > 2
            ? ingredients.get(2).orElse(original.additionIngredient().orElseThrow())
            : original.additionIngredient().orElseThrow();

        return new SmithingTrimRecipe(template, base, addition, original.pattern);
    }
}

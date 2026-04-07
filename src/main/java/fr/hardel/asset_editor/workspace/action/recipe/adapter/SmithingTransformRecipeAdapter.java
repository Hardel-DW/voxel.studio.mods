package fr.hardel.asset_editor.workspace.action.recipe.adapter;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.crafting.TransmuteResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SmithingTransformRecipeAdapter extends RecipeAdapter<SmithingTransformRecipe> {

    private static final Identifier SERIALIZER_ID = Identifier.withDefaultNamespace("smithing_transform");

    public SmithingTransformRecipeAdapter() {
        super(SmithingTransformRecipe.class, SERIALIZER_ID);
    }

    @Override
    protected List<Optional<Ingredient>> doExtractIngredients(SmithingTransformRecipe recipe) {
        return new ArrayList<>(List.of(
            recipe.templateIngredient(),
            Optional.of(recipe.baseIngredient()),
            recipe.additionIngredient()));
    }

    @Override
    protected ItemStack doExtractResult(SmithingTransformRecipe recipe) {
        return new ItemStack(recipe.result.item(), recipe.result.count(), recipe.result.components());
    }

    @Override
    protected SmithingTransformRecipe doRebuild(SmithingTransformRecipe original, List<Optional<Ingredient>> ingredients) {
        Optional<Ingredient> template = !ingredients.isEmpty() ? ingredients.get(0) : original.templateIngredient();
        Ingredient base = ingredients.size() > 1 ? ingredients.get(1).orElse(original.baseIngredient()) : original.baseIngredient();
        Optional<Ingredient> addition = ingredients.size() > 2 ? ingredients.get(2) : original.additionIngredient();
        return new SmithingTransformRecipe(template, base, addition, original.result);
    }

    @Override
    protected SmithingTransformRecipe doSetResultCount(SmithingTransformRecipe recipe, int count) {
        return new SmithingTransformRecipe(
            recipe.templateIngredient(),
            recipe.baseIngredient(),
            recipe.additionIngredient(),
            new TransmuteResult(recipe.result.item(), count, recipe.result.components()));
    }

    @Override
    protected SmithingTransformRecipe doSetResultItem(SmithingTransformRecipe recipe, Holder<Item> item) {
        return new SmithingTransformRecipe(
            recipe.templateIngredient(),
            recipe.baseIngredient(),
            recipe.additionIngredient(),
            replaceTransmuteResult(item, recipe.result.count(), recipe.result.components()));
    }

    @Override
    public boolean supportsResultCount() {
        return true;
    }

    @Override
    public Recipe<?> buildFromGeneric(List<Optional<Ingredient>> ingredients, ItemStack result) {
        Optional<Ingredient> template = !ingredients.isEmpty() ? ingredients.get(0) : Optional.empty();
        Ingredient base = ingredients.size() > 1
            ? ingredients.get(1).orElse(Ingredient.of(Items.STONE))
            : Ingredient.of(Items.STONE);
        Optional<Ingredient> addition = ingredients.size() > 2 ? ingredients.get(2) : Optional.empty();

        return new SmithingTransformRecipe(
            template, base, addition,
            new TransmuteResult(result.getItemHolder(), result.getCount(), result.getComponentsPatch()));
    }
}

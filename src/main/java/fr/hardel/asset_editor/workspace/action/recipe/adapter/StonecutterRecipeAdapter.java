package fr.hardel.asset_editor.workspace.action.recipe.adapter;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class StonecutterRecipeAdapter extends RecipeAdapter<StonecutterRecipe> {

    private static final Identifier SERIALIZER_ID = Identifier.withDefaultNamespace("stonecutting");

    public StonecutterRecipeAdapter() {
        super(StonecutterRecipe.class, SERIALIZER_ID);
    }

    @Override
    protected List<Optional<Ingredient>> doExtractIngredients(StonecutterRecipe recipe) {
        return new ArrayList<>(List.of(Optional.of(recipe.input())));
    }

    @Override
    protected ItemStack doExtractResult(StonecutterRecipe recipe) {
        return recipe.result().copy();
    }

    @Override
    protected StonecutterRecipe doRebuild(StonecutterRecipe original, List<Optional<Ingredient>> ingredients) {
        Ingredient input = ingredients.isEmpty()
            ? original.input()
            : ingredients.getFirst().orElse(original.input());

        return new StonecutterRecipe(original.group(), input, original.result().copy());
    }

    @Override
    protected StonecutterRecipe doSetProperty(StonecutterRecipe recipe, String key, Object value) {
        return switch (key) {
            case GROUP -> new StonecutterRecipe((String) value, recipe.input(), recipe.result().copy());
            default -> null;
        };
    }

    @Override
    protected StonecutterRecipe doSetResultCount(StonecutterRecipe recipe, int count) {
        return new StonecutterRecipe(recipe.group(), recipe.input(), recipe.result().copyWithCount(count));
    }

    @Override
    protected StonecutterRecipe doSetResultItem(StonecutterRecipe recipe, Holder<Item> item) {
        return new StonecutterRecipe(recipe.group(), recipe.input(), replaceResultStack(item, recipe.result().getCount(), recipe.result().getComponentsPatch()));
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

        return new StonecutterRecipe("", input, result);
    }
}

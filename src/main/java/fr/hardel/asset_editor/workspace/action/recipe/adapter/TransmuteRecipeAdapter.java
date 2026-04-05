package fr.hardel.asset_editor.workspace.action.recipe.adapter;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.TransmuteRecipe;
import net.minecraft.world.item.crafting.TransmuteResult;

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
        Ingredient input = !ingredients.isEmpty() ? ingredients.get(0).orElse(original.input) : original.input;
        Ingredient material = ingredients.size() > 1 ? ingredients.get(1).orElse(original.material) : original.material;
        return new TransmuteRecipe(original.group(), original.category(), input, material, original.result);
    }

    @Override
    protected TransmuteRecipe doSetResultCount(TransmuteRecipe recipe, int count) {
        return new TransmuteRecipe(
            recipe.group(),
            recipe.category(),
            recipe.input,
            recipe.material,
            new TransmuteResult(recipe.result.item(), count, recipe.result.components())
        );
    }

    @Override
    protected TransmuteRecipe doSetResultItem(TransmuteRecipe recipe, Holder<Item> item) {
        return new TransmuteRecipe(
            recipe.group(),
            recipe.category(),
            recipe.input,
            recipe.material,
            replaceTransmuteResult(item, recipe.result.count(), recipe.result.components())
        );
    }

    @Override
    public boolean supportsResultCount() {
        return true;
    }

    @Override
    public boolean supportsGroup() {
        return true;
    }

    @Override
    public boolean supportsCraftingCategory() {
        return true;
    }

    @Override
    protected TransmuteRecipe doSetGroup(TransmuteRecipe recipe, String group) {
        return new TransmuteRecipe(group, recipe.category(), recipe.input, recipe.material, recipe.result);
    }

    @Override
    protected TransmuteRecipe doSetCraftingCategory(TransmuteRecipe recipe, CraftingBookCategory category) {
        return new TransmuteRecipe(recipe.group(), category, recipe.input, recipe.material, recipe.result);
    }

    @Override
    public Recipe<?> buildFromGeneric(List<Optional<Ingredient>> ingredients, ItemStack result) {
        Ingredient input = ingredients.stream()
            .flatMap(Optional::stream)
            .findFirst()
            .orElse(Ingredient.of(Items.STONE));

        Ingredient material = ingredients.stream()
            .flatMap(Optional::stream)
            .skip(1)
            .findFirst()
            .orElse(input);

        return new TransmuteRecipe(
            "", CraftingBookCategory.MISC, input, material,
            new TransmuteResult(result.getItemHolder(), result.getCount(), result.getComponentsPatch())
        );
    }
}

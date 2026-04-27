package fr.hardel.asset_editor.workspace.action.recipe.adapter;

import fr.hardel.asset_editor.workspace.action.recipe.RecipeIngredientHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ShapelessRecipeAdapter extends RecipeAdapter<ShapelessRecipe> {

    public static final String CATEGORY = "category";

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
        if (compacted.isEmpty()) return original;
        return new ShapelessRecipe(original.group(), original.category(), original.result.copy(), compacted);
    }

    @Override
    protected ShapelessRecipe doApplyPattern(ShapelessRecipe original, Map<Integer, List<Identifier>> slots, RecipeIngredientHelper helper) {
        List<Ingredient> compacted = compactSlots(slots, helper);
        if (compacted.isEmpty()) return original;
        return new ShapelessRecipe(original.group(), original.category(), original.result.copy(), compacted);
    }

    @Override
    protected Map<String, Object> extractProperties(ShapelessRecipe recipe) {
        var props = super.extractProperties(recipe);
        props.put(CATEGORY, recipe.category().getSerializedName());
        return props;
    }

    @Override
    protected ShapelessRecipe doSetProperty(ShapelessRecipe recipe, String key, Object value) {
        return switch (key) {
            case GROUP -> new ShapelessRecipe((String) value, recipe.category(), recipe.result.copy(), recipe.ingredients);
            case CATEGORY -> {
                var cat = Arrays.stream(CraftingBookCategory.values())
                    .filter(c -> c.getSerializedName().equals(value))
                    .findFirst().orElse(null);
                yield cat != null ? new ShapelessRecipe(recipe.group(), cat, recipe.result.copy(), recipe.ingredients) : null;
            }
            default -> null;
        };
    }

    @Override
    protected ShapelessRecipe doSetResultCount(ShapelessRecipe recipe, int count) {
        return new ShapelessRecipe(recipe.group(), recipe.category(), recipe.result.copyWithCount(count), recipe.ingredients);
    }

    @Override
    protected ShapelessRecipe doSetResultItem(ShapelessRecipe recipe, Holder<Item> item) {
        return new ShapelessRecipe(recipe.group(), recipe.category(), replaceResultStack(item, recipe.result.getCount(), recipe.result.getComponentsPatch()), recipe.ingredients);
    }

    @Override
    protected ShapelessRecipe doSetResultComponents(ShapelessRecipe recipe, DataComponentPatch patch) {
        ItemStack updated = validatedResultStack(recipe.result.getItemHolder(), recipe.result.getCount(), patch);
        if (updated == null) return null;
        return new ShapelessRecipe(recipe.group(), recipe.category(), updated, recipe.ingredients);
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

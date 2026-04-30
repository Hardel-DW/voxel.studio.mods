package fr.hardel.asset_editor.workspace.action.recipe.adapter;

import fr.hardel.asset_editor.workspace.action.recipe.RecipeIngredientHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CookingRecipeAdapter<T extends AbstractCookingRecipe> extends RecipeAdapter<T> {

    public static final String CATEGORY = "category";
    public static final String EXPERIENCE = "experience";
    public static final String COOKING_TIME = "cooking_time";

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

        return factory.create(original.group(), original.category(), input, original.result().copy(), original.experience(), original.cookingTime());
    }

    @Override
    protected T doApplyPattern(T original, Map<Integer, List<Identifier>> slots, RecipeIngredientHelper helper) {
        Ingredient input = indexedSlots(slots, 1, helper).getFirst().orElse(original.input());
        return factory.create(original.group(), original.category(), input, original.result().copy(), original.experience(), original.cookingTime());
    }

    @Override
    protected Map<String, Object> extractProperties(T recipe) {
        var props = super.extractProperties(recipe);
        props.put(CATEGORY, recipe.category().getSerializedName());
        props.put(EXPERIENCE, recipe.experience());
        props.put(COOKING_TIME, recipe.cookingTime());
        return props;
    }

    @Override
    protected T doSetProperty(T recipe, String key, Object value) {
        return switch (key) {
            case GROUP -> factory.create((String) value, recipe.category(), recipe.input(), recipe.result().copy(), recipe.experience(), recipe.cookingTime());
            case CATEGORY -> {
                var cat = Arrays.stream(CookingBookCategory.values())
                    .filter(c -> c.getSerializedName().equals(value))
                    .findFirst().orElse(null);
                yield cat != null ? factory.create(recipe.group(), cat, recipe.input(), recipe.result().copy(), recipe.experience(), recipe.cookingTime()) : null;
            }
            case EXPERIENCE -> factory.create(recipe.group(), recipe.category(), recipe.input(), recipe.result().copy(), (float) value, recipe.cookingTime());
            case COOKING_TIME -> factory.create(recipe.group(), recipe.category(), recipe.input(), recipe.result().copy(), recipe.experience(), (int) value);
            default -> null;
        };
    }

    @Override
    protected T doSetResultCount(T recipe, int count) {
        return factory.create(recipe.group(), recipe.category(), recipe.input(), recipe.result().copyWithCount(count), recipe.experience(), recipe.cookingTime());
    }

    @Override
    protected T doSetResultItem(T recipe, Holder<Item> item) {
        return factory.create(recipe.group(), recipe.category(), recipe.input(), replaceResultStack(item, recipe.result().getCount(), recipe.result().getComponentsPatch()), recipe.experience(), recipe.cookingTime());
    }

    @Override
    protected T doSetResultComponents(T recipe, DataComponentPatch patch) {
        ItemStack updated = validatedResultStack(recipe.result().getItemHolder(), recipe.result().getCount(), patch);
        if (updated == null) return null;
        return factory.create(recipe.group(), recipe.category(), recipe.input(), updated, recipe.experience(), recipe.cookingTime());
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

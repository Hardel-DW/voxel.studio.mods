package fr.hardel.asset_editor.workspace.action.recipe.adapter;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ShapedRecipeAdapter extends RecipeAdapter<ShapedRecipe> {

    private static final Identifier SERIALIZER_ID = Identifier.withDefaultNamespace("crafting_shaped");

    public ShapedRecipeAdapter() {
        super(ShapedRecipe.class, SERIALIZER_ID);
    }

    @Override
    protected List<Optional<Ingredient>> doExtractIngredients(ShapedRecipe recipe) {
        return new ArrayList<>(recipe.getIngredients());
    }

    @Override
    protected ItemStack doExtractResult(ShapedRecipe recipe) {
        return recipe.result.copy();
    }

    @Override
    protected ShapedRecipe doRebuild(ShapedRecipe original, List<Optional<Ingredient>> ingredients) {
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();
        int totalSlots = originalWidth * originalHeight;

        List<Optional<Ingredient>> padded = new ArrayList<>(ingredients);
        while (padded.size() < totalSlots) {
            padded.add(Optional.empty());
        }

        BoundingBox box = computeBoundingBox(padded, originalWidth, originalHeight);
        List<Optional<Ingredient>> shrunk = extractRegion(padded, originalWidth, box);

        ShapedRecipePattern pattern = new ShapedRecipePattern(box.width(), box.height(), shrunk, Optional.empty());
        return new ShapedRecipe(original.group(), original.category(), pattern, original.result.copy(), original.showNotification());
    }

    @Override
    public @Nullable Recipe<?> convertFrom(Recipe<?> source, boolean preserveIngredients) {
        if (!(source instanceof CraftingRecipe)) return null;

        List<Ingredient> ingredients = preserveIngredients
            ? RecipeAdapterRegistry.extractIngredients(source).stream().flatMap(Optional::stream).toList()
            : List.of();

        ItemStack result = RecipeAdapterRegistry.extractResult(source);

        List<Optional<Ingredient>> grid = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            grid.add(i < ingredients.size() ? Optional.of(ingredients.get(i)) : Optional.empty());
        }

        BoundingBox box = computeBoundingBox(grid, 3, 3);
        List<Optional<Ingredient>> shrunk = extractRegion(grid, 3, box);

        ShapedRecipePattern pattern = new ShapedRecipePattern(box.width(), box.height(), shrunk, Optional.empty());
        return new ShapedRecipe(source.group(), CraftingBookCategory.MISC, pattern, result, true);
    }

    private record BoundingBox(int minX, int minY, int width, int height) {}

    private static BoundingBox computeBoundingBox(List<Optional<Ingredient>> grid, int gridWidth, int gridHeight) {
        int minX = gridWidth, maxX = -1, minY = gridHeight, maxY = -1;

        for (int i = 0; i < grid.size(); i++) {
            if (grid.get(i).isPresent()) {
                int x = i % gridWidth;
                int y = i / gridWidth;
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
            }
        }

        if (maxX < 0) {
            return new BoundingBox(0, 0, 1, 1);
        }

        return new BoundingBox(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private static List<Optional<Ingredient>> extractRegion(List<Optional<Ingredient>> grid, int gridWidth, BoundingBox box) {
        List<Optional<Ingredient>> result = new ArrayList<>(box.width() * box.height());
        for (int y = box.minY(); y < box.minY() + box.height(); y++) {
            for (int x = box.minX(); x < box.minX() + box.width(); x++) {
                int index = y * gridWidth + x;
                result.add(index < grid.size() ? grid.get(index) : Optional.empty());
            }
        }
        return result;
    }
}

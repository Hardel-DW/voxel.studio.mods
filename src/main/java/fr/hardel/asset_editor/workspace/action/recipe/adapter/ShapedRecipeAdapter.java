package fr.hardel.asset_editor.workspace.action.recipe.adapter;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ShapedRecipeAdapter extends RecipeAdapter<ShapedRecipe> {

    private static final Identifier SERIALIZER_ID = Identifier.withDefaultNamespace("crafting_shaped");
    private static final char[] SYMBOLS = "ABCDEFGHI".toCharArray();

    public ShapedRecipeAdapter() {
        super(ShapedRecipe.class, SERIALIZER_ID);
    }

    @Override
    protected List<Optional<Ingredient>> doExtractIngredients(ShapedRecipe recipe) {
        List<Optional<Ingredient>> expanded = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            expanded.add(Optional.empty());
        }

        List<Optional<Ingredient>> packed = recipe.getIngredients();
        int width = recipe.getWidth();
        int height = recipe.getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                expanded.set(y * 3 + x, packed.get(y * width + x));
            }
        }

        return expanded;
    }

    @Override
    protected ItemStack doExtractResult(ShapedRecipe recipe) {
        return recipe.result.copy();
    }

    @Override
    protected ShapedRecipe doRebuild(ShapedRecipe original, List<Optional<Ingredient>> ingredients) {
        List<Optional<Ingredient>> padded = new ArrayList<>(ingredients.subList(0, Math.min(ingredients.size(), 9)));
        while (padded.size() < 9) {
            padded.add(Optional.empty());
        }

        BoundingBox box = computeBoundingBox(padded, 3, 3);
        List<Optional<Ingredient>> shrunk = extractRegion(padded, 3, box);
        ShapedRecipePattern pattern = packPattern(shrunk, box.width());
        return new ShapedRecipe(original.group(), original.category(), pattern, original.result.copy(), original.showNotification());
    }

    @Override
    protected ShapedRecipe doSetResultCount(ShapedRecipe recipe, int count) {
        List<Optional<Ingredient>> ingredients = doExtractIngredients(recipe);
        BoundingBox box = computeBoundingBox(ingredients, 3, 3);
        List<Optional<Ingredient>> shrunk = extractRegion(ingredients, 3, box);
        return new ShapedRecipe(
            recipe.group(),
            recipe.category(),
            packPattern(shrunk, box.width()),
            recipe.result.copyWithCount(count),
            recipe.showNotification()
        );
    }

    @Override
    public boolean supportsResultCount() {
        return true;
    }

    @Override
    public Recipe<?> buildFromGeneric(List<Optional<Ingredient>> ingredients, ItemStack result) {
        List<Optional<Ingredient>> grid = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            grid.add(i < ingredients.size() ? ingredients.get(i) : Optional.empty());
        }

        BoundingBox box = computeBoundingBox(grid, 3, 3);
        List<Optional<Ingredient>> shrunk = extractRegion(grid, 3, box);
        ShapedRecipePattern pattern = packPattern(shrunk, box.width());
        return new ShapedRecipe("", CraftingBookCategory.MISC, pattern, result, true);
    }

    private static ShapedRecipePattern packPattern(List<Optional<Ingredient>> ingredients, int width) {
        if (ingredients.isEmpty() || ingredients.stream().noneMatch(Optional::isPresent))
            throw new IllegalArgumentException("Shaped recipes require at least one ingredient");

        Map<Character, Ingredient> key = new LinkedHashMap<>();
        List<String> rows = new ArrayList<>();
        StringBuilder row = new StringBuilder(width);
        int symbolIndex = 0;

        for (int i = 0; i < ingredients.size(); i++) {
            Optional<Ingredient> ingredient = ingredients.get(i);
            if (ingredient.isPresent()) {
                Character existing = null;
                for (Map.Entry<Character, Ingredient> entry : key.entrySet()) {
                    if (entry.getValue().equals(ingredient.get())) {
                        existing = entry.getKey();
                        break;
                    }
                }

                char symbol = existing != null ? existing : SYMBOLS[symbolIndex++];
                key.putIfAbsent(symbol, ingredient.get());
                row.append(symbol);
            } else {
                row.append(ShapedRecipePattern.EMPTY_SLOT);
            }

            if ((i + 1) % width == 0) {
                rows.add(row.toString());
                row = new StringBuilder(width);
            }
        }

        return ShapedRecipePattern.of(key, rows);
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

package fr.hardel.asset_editor.workspace.action.recipe;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class RecipeIngredientHelper {

    private final net.minecraft.core.HolderLookup.Provider registries;

    public RecipeIngredientHelper(net.minecraft.core.HolderLookup.Provider registries) {
        this.registries = registries;
    }

    public Holder<Item> resolveItem(Identifier id) {
        var lookup = registries.lookupOrThrow(Registries.ITEM);
        return lookup.getOrThrow(ResourceKey.create(Registries.ITEM, id));
    }

    public Ingredient toIngredient(List<Identifier> itemIds) {
        List<Holder<Item>> holders = itemIds.stream().map(this::resolveItem).toList();
        return Ingredient.of(HolderSet.direct(holders));
    }

    public Ingredient merge(Ingredient existing, List<Identifier> newItemIds) {
        Set<Identifier> seen = new LinkedHashSet<>();
        List<Holder<Item>> merged = new ArrayList<>();

        existing.items().forEach(holder -> {
            Identifier id = holder.unwrapKey()
                .map(ResourceKey::identifier)
                .orElse(null);
            if (id != null && seen.add(id)) {
                merged.add(holder);
            }
        });

        for (Identifier newId : newItemIds) {
            if (seen.add(newId)) {
                merged.add(resolveItem(newId));
            }
        }

        return Ingredient.of(HolderSet.direct(merged));
    }

    public @Nullable Ingredient remove(Ingredient existing, List<Identifier> toRemove) {
        Set<Identifier> removeSet = new HashSet<>(toRemove);
        List<Holder<Item>> remaining = existing.items()
            .filter(holder -> holder.unwrapKey()
                .map(key -> !removeSet.contains(key.identifier()))
                .orElse(true))
            .toList();

        if (remaining.isEmpty()) {
            return null;
        }
        return Ingredient.of(HolderSet.direct(remaining));
    }

    public @Nullable Ingredient replace(Ingredient existing, Identifier from, Identifier to) {
        Holder<Item> toHolder = resolveItem(to);
        Set<Identifier> seen = new LinkedHashSet<>();
        List<Holder<Item>> replaced = new ArrayList<>();

        existing.items().forEach(holder -> {
            Identifier id = holder.unwrapKey()
                .map(ResourceKey::identifier)
                .orElse(null);
            if (id == null) return;

            Holder<Item> target = id.equals(from) ? toHolder : holder;
            Identifier targetId = target.unwrapKey()
                .map(ResourceKey::identifier)
                .orElse(id);

            if (seen.add(targetId)) {
                replaced.add(target);
            }
        });

        if (replaced.isEmpty()) {
            return null;
        }
        return Ingredient.of(HolderSet.direct(replaced));
    }

    // -- Extraction polymorphe --

    public List<Optional<Ingredient>> extractIngredients(Recipe<?> recipe) {
        return switch (recipe) {
            case ShapedRecipe shaped -> new ArrayList<>(shaped.getIngredients());
            case ShapelessRecipe shapeless -> shapeless.ingredients.stream()
                .map(Optional::of)
                .collect(Collectors.toCollection(ArrayList::new));
            case AbstractCookingRecipe cooking -> new ArrayList<>(List.of(Optional.of(cooking.input())));
            case StonecutterRecipe stonecutter -> new ArrayList<>(List.of(Optional.of(stonecutter.input())));
            case SmithingTransformRecipe smithing -> new ArrayList<>(List.of(
                smithing.templateIngredient(),
                Optional.of(smithing.baseIngredient()),
                smithing.additionIngredient()
            ));
            case SmithingTrimRecipe trim -> new ArrayList<>(List.of(
                trim.templateIngredient(),
                Optional.of(trim.baseIngredient()),
                trim.additionIngredient()
            ));
            case TransmuteRecipe transmute -> new ArrayList<>(List.of(
                Optional.of(transmute.input),
                Optional.of(transmute.material)
            ));
            default -> new ArrayList<>();
        };
    }

    public ItemStack extractResult(Recipe<?> recipe) {
        return switch (recipe) {
            case ShapedRecipe shaped -> shaped.result.copy();
            case ShapelessRecipe shapeless -> shapeless.result.copy();
            case SmithingTransformRecipe smithing ->
                new ItemStack(smithing.result.item(), smithing.result.count(), smithing.result.components());
            case TransmuteRecipe transmute ->
                new ItemStack(transmute.result.item(), transmute.result.count(), transmute.result.components());
            case SingleItemRecipe single -> single.result().copy();
            default -> ItemStack.EMPTY;
        };
    }

    // -- Reconstruction polymorphe --

    public Recipe<?> rebuild(Recipe<?> original, List<Optional<Ingredient>> ingredients) {
        return switch (original) {
            case ShapedRecipe shaped -> rebuildShaped(shaped, ingredients);
            case ShapelessRecipe shapeless -> rebuildShapeless(shapeless, ingredients);
            case SmeltingRecipe r -> rebuildCooking(r, ingredients, SmeltingRecipe::new);
            case BlastingRecipe r -> rebuildCooking(r, ingredients, BlastingRecipe::new);
            case SmokingRecipe r -> rebuildCooking(r, ingredients, SmokingRecipe::new);
            case CampfireCookingRecipe r -> rebuildCooking(r, ingredients, CampfireCookingRecipe::new);
            case StonecutterRecipe r -> rebuildStonecutter(r, ingredients);
            case SmithingTransformRecipe r -> rebuildSmithingTransform(r, ingredients);
            case SmithingTrimRecipe r -> rebuildSmithingTrim(r, ingredients);
            case TransmuteRecipe r -> rebuildTransmute(r, ingredients);
            default -> original;
        };
    }

    private ShapedRecipe rebuildShaped(ShapedRecipe original, List<Optional<Ingredient>> ingredients) {
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();

        List<Optional<Ingredient>> padded = new ArrayList<>(ingredients);
        int totalSlots = originalWidth * originalHeight;
        while (padded.size() < totalSlots) {
            padded.add(Optional.empty());
        }

        BoundingBox box = computeBoundingBox(padded, originalWidth, originalHeight);
        List<Optional<Ingredient>> shrunk = extractRegion(padded, originalWidth, box);

        ShapedRecipePattern pattern = new ShapedRecipePattern(
            box.width(), box.height(), shrunk, Optional.empty()
        );
        return new ShapedRecipe(original.group(), original.category(), pattern, original.result.copy(), original.showNotification());
    }

    private ShapelessRecipe rebuildShapeless(ShapelessRecipe original, List<Optional<Ingredient>> ingredients) {
        List<Ingredient> compacted = ingredients.stream()
            .flatMap(Optional::stream)
            .toList();
        return new ShapelessRecipe(original.group(), original.category(), original.result.copy(), compacted);
    }

    private <T extends AbstractCookingRecipe> T rebuildCooking(
        T original, List<Optional<Ingredient>> ingredients, AbstractCookingRecipe.Factory<T> factory
    ) {
        Ingredient input = ingredients.isEmpty() ? original.input()
            : ingredients.getFirst().orElse(original.input());
        return factory.create(
            original.group(), original.category(), input,
            original.result().copy(), original.experience(), original.cookingTime()
        );
    }

    private StonecutterRecipe rebuildStonecutter(StonecutterRecipe original, List<Optional<Ingredient>> ingredients) {
        Ingredient input = ingredients.isEmpty() ? original.input()
            : ingredients.getFirst().orElse(original.input());
        return new StonecutterRecipe(original.group(), input, original.result().copy());
    }

    private SmithingTransformRecipe rebuildSmithingTransform(SmithingTransformRecipe original, List<Optional<Ingredient>> ingredients) {
        Optional<Ingredient> template = ingredients.size() > 0 ? ingredients.get(0) : original.templateIngredient();
        Ingredient base = ingredients.size() > 1 ? ingredients.get(1).orElse(original.baseIngredient()) : original.baseIngredient();
        Optional<Ingredient> addition = ingredients.size() > 2 ? ingredients.get(2) : original.additionIngredient();
        return new SmithingTransformRecipe(template, base, addition, original.result);
    }

    private SmithingTrimRecipe rebuildSmithingTrim(SmithingTrimRecipe original, List<Optional<Ingredient>> ingredients) {
        Ingredient template = ingredients.size() > 0 ? ingredients.get(0).orElse(original.templateIngredient().orElse(null)) : original.templateIngredient().orElse(null);
        Ingredient base = ingredients.size() > 1 ? ingredients.get(1).orElse(original.baseIngredient()) : original.baseIngredient();
        Ingredient addition = ingredients.size() > 2 ? ingredients.get(2).orElse(original.additionIngredient().orElse(null)) : original.additionIngredient().orElse(null);
        if (template == null || addition == null) return original;
        return new SmithingTrimRecipe(template, base, addition, original.pattern);
    }

    private TransmuteRecipe rebuildTransmute(TransmuteRecipe original, List<Optional<Ingredient>> ingredients) {
        Ingredient input = ingredients.size() > 0 ? ingredients.get(0).orElse(original.input) : original.input;
        Ingredient material = ingredients.size() > 1 ? ingredients.get(1).orElse(original.material) : original.material;
        return new TransmuteRecipe(original.group(), original.category(), input, material, original.result);
    }

    // -- Conversion entre types de la même famille --

    public @Nullable Recipe<?> convertCraftingType(Recipe<?> recipe, Identifier newSerializer, boolean preserveIngredients) {
        boolean isCraftingSource = recipe instanceof CraftingRecipe;
        boolean isCookingSource = recipe instanceof AbstractCookingRecipe;

        String serializerId = newSerializer.toString();
        ItemStack result = extractResult(recipe);

        if (isCraftingSource && isCraftingSerializer(serializerId)) {
            List<Ingredient> preserved = preserveIngredients
                ? collectNonEmptyIngredients(recipe)
                : List.of();
            return buildCraftingRecipe(serializerId, recipe.group(), preserved, result);
        }

        if (isCookingSource && isCookingSerializer(serializerId)) {
            AbstractCookingRecipe cooking = (AbstractCookingRecipe) recipe;
            Ingredient input = preserveIngredients ? cooking.input() : Ingredient.of(net.minecraft.world.item.Items.STONE);
            return buildCookingRecipe(serializerId, recipe.group(), input, result, cooking.experience());
        }

        return null;
    }

    private List<Ingredient> collectNonEmptyIngredients(Recipe<?> recipe) {
        return extractIngredients(recipe).stream()
            .flatMap(Optional::stream)
            .toList();
    }

    private boolean isCraftingSerializer(String id) {
        return id.equals("minecraft:crafting_shaped")
            || id.equals("minecraft:crafting_shapeless");
    }

    private boolean isCookingSerializer(String id) {
        return id.equals("minecraft:smelting")
            || id.equals("minecraft:blasting")
            || id.equals("minecraft:smoking")
            || id.equals("minecraft:campfire_cooking");
    }

    private Recipe<?> buildCraftingRecipe(String serializerId, String group, List<Ingredient> ingredients, ItemStack result) {
        if (serializerId.equals("minecraft:crafting_shapeless")) {
            List<Ingredient> capped = ingredients.isEmpty() ? List.of() : ingredients.subList(0, Math.min(ingredients.size(), 9));
            return new ShapelessRecipe(group, CraftingBookCategory.MISC, result, capped);
        }

        List<Optional<Ingredient>> grid = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            grid.add(i < ingredients.size() ? Optional.of(ingredients.get(i)) : Optional.empty());
        }

        BoundingBox box = computeBoundingBox(grid, 3, 3);
        List<Optional<Ingredient>> shrunk = extractRegion(grid, 3, box);

        ShapedRecipePattern pattern = new ShapedRecipePattern(box.width(), box.height(), shrunk, Optional.empty());
        return new ShapedRecipe(group, CraftingBookCategory.MISC, pattern, result, true);
    }

    private AbstractCookingRecipe buildCookingRecipe(String serializerId, String group, Ingredient input, ItemStack result, float experience) {
        return switch (serializerId) {
            case "minecraft:smelting" -> new SmeltingRecipe(group, CookingBookCategory.MISC, input, result, experience, 200);
            case "minecraft:blasting" -> new BlastingRecipe(group, CookingBookCategory.MISC, input, result, experience, 100);
            case "minecraft:smoking" -> new SmokingRecipe(group, CookingBookCategory.MISC, input, result, experience, 100);
            case "minecraft:campfire_cooking" -> new CampfireCookingRecipe(group, CookingBookCategory.MISC, input, result, experience, 600);
            default -> new SmeltingRecipe(group, CookingBookCategory.MISC, input, result, experience, 200);
        };
    }

    // -- Bounding box pour ShapedRecipe --

    private record BoundingBox(int minX, int minY, int width, int height) {}

    private BoundingBox computeBoundingBox(List<Optional<Ingredient>> grid, int gridWidth, int gridHeight) {
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

    private List<Optional<Ingredient>> extractRegion(List<Optional<Ingredient>> grid, int gridWidth, BoundingBox box) {
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

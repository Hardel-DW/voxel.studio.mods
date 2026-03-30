package fr.hardel.asset_editor.workspace.registry.impl;

import fr.hardel.asset_editor.store.ElementEntry;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import fr.hardel.asset_editor.workspace.action.recipe.RecipeEditorActions;
import fr.hardel.asset_editor.workspace.action.recipe.RecipeIngredientHelper;
import fr.hardel.asset_editor.workspace.registry.MutationActionHandler;
import fr.hardel.asset_editor.workspace.registry.MutationHandlerRegistry;
import fr.hardel.asset_editor.workspace.registry.RegistryMutationContext;
import fr.hardel.asset_editor.workspace.registry.RegistryMutationDispatcher;
import fr.hardel.asset_editor.workspace.registry.RegistryMutationHandler;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class RecipeMutationHandler implements RegistryMutationHandler<Recipe<?>> {

    private static final RegistryMutationDispatcher<Recipe<?>> DISPATCHER = createDispatcher();

    public static void register() {
        MutationHandlerRegistry.register(Registries.RECIPE, new RecipeMutationHandler());
    }

    @Override
    public void beforeApply(EditorAction action, RegistryMutationContext context) {
        DISPATCHER.beforeApply(action, context);
    }

    @Override
    public ElementEntry<Recipe<?>> apply(ElementEntry<Recipe<?>> entry, EditorAction action, RegistryMutationContext context) {
        return DISPATCHER.apply(entry, action, context);
    }

    private static RegistryMutationDispatcher<Recipe<?>> createDispatcher() {
        return RegistryMutationHandler.<Recipe<?>>dispatcher()
            .register(
                RecipeEditorActions.ADD_INGREDIENT,
                MutationActionHandler.of(RecipeMutationHandler::applyAddIngredient)
            )
            .register(
                RecipeEditorActions.ADD_SHAPELESS_INGREDIENT,
                MutationActionHandler.of(RecipeMutationHandler::applyAddShapelessIngredient)
            )
            .register(
                RecipeEditorActions.REMOVE_INGREDIENT,
                MutationActionHandler.of(RecipeMutationHandler::applyRemoveIngredient)
            )
            .register(
                RecipeEditorActions.REMOVE_ITEM_EVERYWHERE,
                MutationActionHandler.of(RecipeMutationHandler::applyRemoveItemEverywhere)
            )
            .register(
                RecipeEditorActions.REPLACE_ITEM_EVERYWHERE,
                MutationActionHandler.of(RecipeMutationHandler::applyReplaceItemEverywhere)
            )
            .register(
                RecipeEditorActions.CONVERT_RECIPE_TYPE,
                MutationActionHandler.of(RecipeMutationHandler::applyConvertRecipeType)
            )
            .register(
                RecipeEditorActions.SET_RESULT_COUNT,
                MutationActionHandler.of(RecipeMutationHandler::applySetResultCount)
            )
            .register(
                RecipeEditorActions.SET_RESULT_ITEM,
                MutationActionHandler.of(RecipeMutationHandler::applySetResultItem)
            )
            .register(
                RecipeEditorActions.SET_GROUP,
                MutationActionHandler.of(RecipeMutationHandler::applySetGroup)
            )
            .register(
                RecipeEditorActions.SET_CATEGORY,
                MutationActionHandler.of(RecipeMutationHandler::applySetCategory)
            )
            .register(
                RecipeEditorActions.SET_COOKING_EXPERIENCE,
                MutationActionHandler.of(RecipeMutationHandler::applySetCookingExperience)
            )
            .register(
                RecipeEditorActions.SET_COOKING_TIME,
                MutationActionHandler.of(RecipeMutationHandler::applySetCookingTime)
            )
            .register(
                RecipeEditorActions.SET_SHOW_NOTIFICATION,
                MutationActionHandler.of(RecipeMutationHandler::applySetShowNotification)
            );
    }

    private static ElementEntry<Recipe<?>> applyAddIngredient(
        ElementEntry<Recipe<?>> entry, RecipeEditorActions.AddIngredient action, RegistryMutationContext context
    ) {
        RecipeIngredientHelper helper = new RecipeIngredientHelper(context.registries());
        Recipe<?> recipe = entry.data();

        if (recipe instanceof ShapelessRecipe) return entry;

        List<Optional<Ingredient>> ingredients = helper.extractIngredients(recipe);
        if (action.slot() < 0 || action.slot() >= ingredients.size()) return entry;

        Optional<Ingredient> existing = ingredients.get(action.slot());
        if (action.replace() || existing.isEmpty()) {
            ingredients.set(action.slot(), Optional.of(helper.toIngredient(action.items())));
        } else {
            ingredients.set(action.slot(), Optional.of(helper.merge(existing.get(), action.items())));
        }

        return entry.withData(helper.rebuild(recipe, ingredients));
    }

    private static ElementEntry<Recipe<?>> applyAddShapelessIngredient(
        ElementEntry<Recipe<?>> entry, RecipeEditorActions.AddShapelessIngredient action, RegistryMutationContext context
    ) {
        if (!(entry.data() instanceof ShapelessRecipe shapeless)) return entry;

        RecipeIngredientHelper helper = new RecipeIngredientHelper(context.registries());
        List<Ingredient> newIngredients = new ArrayList<>(shapeless.ingredients);
        newIngredients.add(helper.toIngredient(action.items()));

        return entry.withData(new ShapelessRecipe(
            shapeless.group(), shapeless.category(), shapeless.result.copy(), newIngredients
        ));
    }

    private static ElementEntry<Recipe<?>> applyRemoveIngredient(
        ElementEntry<Recipe<?>> entry, RecipeEditorActions.RemoveIngredient action, RegistryMutationContext context
    ) {
        RecipeIngredientHelper helper = new RecipeIngredientHelper(context.registries());
        Recipe<?> recipe = entry.data();

        List<Optional<Ingredient>> ingredients = helper.extractIngredients(recipe);
        if (action.slot() < 0 || action.slot() >= ingredients.size()) return entry;

        if (action.items().isEmpty()) {
            ingredients.set(action.slot(), Optional.empty());
        } else {
            Optional<Ingredient> existing = ingredients.get(action.slot());
            if (existing.isEmpty()) return entry;

            Ingredient filtered = helper.remove(existing.get(), action.items());
            ingredients.set(action.slot(), Optional.ofNullable(filtered));
        }

        return entry.withData(helper.rebuild(recipe, ingredients));
    }

    private static ElementEntry<Recipe<?>> applyRemoveItemEverywhere(
        ElementEntry<Recipe<?>> entry, RecipeEditorActions.RemoveItemEverywhere action, RegistryMutationContext context
    ) {
        RecipeIngredientHelper helper = new RecipeIngredientHelper(context.registries());
        Recipe<?> recipe = entry.data();

        List<Optional<Ingredient>> ingredients = helper.extractIngredients(recipe);
        for (int i = 0; i < ingredients.size(); i++) {
            Optional<Ingredient> slot = ingredients.get(i);
            if (slot.isPresent()) {
                Ingredient filtered = helper.remove(slot.get(), action.items());
                ingredients.set(i, Optional.ofNullable(filtered));
            }
        }

        return entry.withData(helper.rebuild(recipe, ingredients));
    }

    private static ElementEntry<Recipe<?>> applyReplaceItemEverywhere(
        ElementEntry<Recipe<?>> entry, RecipeEditorActions.ReplaceItemEverywhere action, RegistryMutationContext context
    ) {
        RecipeIngredientHelper helper = new RecipeIngredientHelper(context.registries());
        Recipe<?> recipe = entry.data();

        List<Optional<Ingredient>> ingredients = helper.extractIngredients(recipe);
        for (int i = 0; i < ingredients.size(); i++) {
            Optional<Ingredient> slot = ingredients.get(i);
            if (slot.isPresent()) {
                Ingredient replaced = helper.replace(slot.get(), action.from(), action.to());
                ingredients.set(i, Optional.ofNullable(replaced));
            }
        }

        return entry.withData(helper.rebuild(recipe, ingredients));
    }

    private static ElementEntry<Recipe<?>> applyConvertRecipeType(
        ElementEntry<Recipe<?>> entry, RecipeEditorActions.ConvertRecipeType action, RegistryMutationContext context
    ) {
        RecipeIngredientHelper helper = new RecipeIngredientHelper(context.registries());
        Recipe<?> converted = helper.convertRecipeType(entry.data(), action.newSerializer(), action.preserveIngredients());

        if (converted == null) return entry;
        return entry.withData(converted);
    }

    private static ElementEntry<Recipe<?>> applySetResultCount(
        ElementEntry<Recipe<?>> entry, RecipeEditorActions.SetResultCount action, RegistryMutationContext context
    ) {
        RecipeIngredientHelper helper = new RecipeIngredientHelper(context.registries());
        int maxCount = Math.max(1, helper.extractResult(entry.data()).getMaxStackSize());
        int count = Math.max(1, Math.min(maxCount, action.count()));
        Recipe<?> updated = helper.setResultCount(entry.data(), count);

        if (updated == null) return entry;
        return entry.withData(updated);
    }

    private static ElementEntry<Recipe<?>> applySetResultItem(
        ElementEntry<Recipe<?>> entry, RecipeEditorActions.SetResultItem action, RegistryMutationContext context
    ) {
        RecipeIngredientHelper helper = new RecipeIngredientHelper(context.registries());
        Recipe<?> updated = helper.setResultItem(entry.data(), action.itemId());

        if (updated == null) return entry;
        return entry.withData(updated);
    }

    private static ElementEntry<Recipe<?>> applySetGroup(
        ElementEntry<Recipe<?>> entry, RecipeEditorActions.SetGroup action, RegistryMutationContext context
    ) {
        RecipeIngredientHelper helper = new RecipeIngredientHelper(context.registries());
        Recipe<?> updated = helper.setGroup(entry.data(), action.group().trim());
        if (updated == null) return entry;
        return entry.withData(updated);
    }

    private static ElementEntry<Recipe<?>> applySetCategory(
        ElementEntry<Recipe<?>> entry, RecipeEditorActions.SetCategory action, RegistryMutationContext context
    ) {
        RecipeIngredientHelper helper = new RecipeIngredientHelper(context.registries());
        Recipe<?> updated = null;

        Optional<CraftingBookCategory> craftingCategory = parseCraftingCategory(action.category());
        if (craftingCategory.isPresent()) {
            updated = helper.setCraftingCategory(entry.data(), craftingCategory.get());
        }

        if (updated == null) {
            Optional<CookingBookCategory> cookingCategory = parseCookingCategory(action.category());
            if (cookingCategory.isPresent()) {
                updated = helper.setCookingCategory(entry.data(), cookingCategory.get());
            }
        }

        if (updated == null) return entry;
        return entry.withData(updated);
    }

    private static ElementEntry<Recipe<?>> applySetCookingExperience(
        ElementEntry<Recipe<?>> entry, RecipeEditorActions.SetCookingExperience action, RegistryMutationContext context
    ) {
        if (!Float.isFinite(action.experience())) return entry;

        RecipeIngredientHelper helper = new RecipeIngredientHelper(context.registries());
        Recipe<?> updated = helper.setCookingExperience(entry.data(), Math.max(0.0F, action.experience()));
        if (updated == null) return entry;
        return entry.withData(updated);
    }

    private static ElementEntry<Recipe<?>> applySetCookingTime(
        ElementEntry<Recipe<?>> entry, RecipeEditorActions.SetCookingTime action, RegistryMutationContext context
    ) {
        RecipeIngredientHelper helper = new RecipeIngredientHelper(context.registries());
        Recipe<?> updated = helper.setCookingTime(entry.data(), sanitizeCookingTime(entry.data(), action.time()));
        if (updated == null) return entry;
        return entry.withData(updated);
    }

    private static ElementEntry<Recipe<?>> applySetShowNotification(
        ElementEntry<Recipe<?>> entry, RecipeEditorActions.SetShowNotification action, RegistryMutationContext context
    ) {
        RecipeIngredientHelper helper = new RecipeIngredientHelper(context.registries());
        Recipe<?> updated = helper.setShowNotification(entry.data(), action.value());
        if (updated == null) return entry;
        return entry.withData(updated);
    }

    private static Optional<CraftingBookCategory> parseCraftingCategory(String category) {
        return Arrays.stream(CraftingBookCategory.values())
            .filter(value -> value.getSerializedName().equals(category))
            .findFirst();
    }

    private static Optional<CookingBookCategory> parseCookingCategory(String category) {
        return Arrays.stream(CookingBookCategory.values())
            .filter(value -> value.getSerializedName().equals(category))
            .findFirst();
    }

    private static int sanitizeCookingTime(Recipe<?> recipe, int time) {
        int min = 1;
        int max = recipe instanceof CampfireCookingRecipe ? Integer.MAX_VALUE : Short.MAX_VALUE;
        return Math.max(min, Math.min(time, max));
    }
}

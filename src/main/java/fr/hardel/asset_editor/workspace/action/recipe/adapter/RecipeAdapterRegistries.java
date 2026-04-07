package fr.hardel.asset_editor.workspace.action.recipe.adapter;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.*;

public final class RecipeAdapterRegistries {

    public static void register() {
        RecipeAdapterRegistry.register(new ShapedRecipeAdapter());
        RecipeAdapterRegistry.register(new ShapelessRecipeAdapter());
        RecipeAdapterRegistry.register(new CookingRecipeAdapter<>(SmeltingRecipe.class, Identifier.withDefaultNamespace("smelting"), SmeltingRecipe::new, 200));
        RecipeAdapterRegistry.register(new CookingRecipeAdapter<>(BlastingRecipe.class, Identifier.withDefaultNamespace("blasting"), BlastingRecipe::new, 100));
        RecipeAdapterRegistry.register(new CookingRecipeAdapter<>(SmokingRecipe.class, Identifier.withDefaultNamespace("smoking"), SmokingRecipe::new, 100));
        RecipeAdapterRegistry.register(new CookingRecipeAdapter<>(CampfireCookingRecipe.class, Identifier.withDefaultNamespace("campfire_cooking"), CampfireCookingRecipe::new, 600));
        RecipeAdapterRegistry.register(new StonecutterRecipeAdapter());
        RecipeAdapterRegistry.register(new SmithingTransformRecipeAdapter());
        RecipeAdapterRegistry.register(new SmithingTrimRecipeAdapter());
        RecipeAdapterRegistry.register(new TransmuteRecipeAdapter());
    }

    private RecipeAdapterRegistries() {}
}

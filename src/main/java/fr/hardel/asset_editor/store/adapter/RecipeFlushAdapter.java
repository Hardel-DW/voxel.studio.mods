package fr.hardel.asset_editor.store.adapter;

import fr.hardel.asset_editor.store.ElementEntry;
import fr.hardel.asset_editor.workspace.action.recipe.adapter.RecipeAdapterRegistry;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;

public enum RecipeFlushAdapter implements FlushAdapter<Recipe<?>> {
    INSTANCE;

    @Override
    public ElementEntry<Recipe<?>> prepare(ElementEntry<Recipe<?>> entry) {
        Recipe<?> recipe = entry.data();
        if (!(recipe instanceof ShapedRecipe) || !RecipeAdapterRegistry.hasAdapter(recipe))
            return entry;

        Recipe<?> canonical = RecipeAdapterRegistry.rebuild(recipe, RecipeAdapterRegistry.extractIngredients(recipe));
        return canonical == recipe ? entry : entry.withData(canonical);
    }
}

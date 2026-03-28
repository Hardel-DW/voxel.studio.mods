package fr.hardel.asset_editor.workspace.action;

import fr.hardel.asset_editor.workspace.action.enchantment.EnchantmentEditorActions;
import fr.hardel.asset_editor.workspace.action.recipe.RecipeEditorActions;

public final class EditorActionRegistries {

    public static void register() {
        EnchantmentEditorActions.register();
        RecipeEditorActions.register();
    }

    private EditorActionRegistries() {
    }
}

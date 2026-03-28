package fr.hardel.asset_editor.workspace.action;

import fr.hardel.asset_editor.workspace.action.enchantment.EnchantmentEditorActions;

public final class EditorActionRegistries {

    public static void register() {
        EnchantmentEditorActions.register();
    }

    private EditorActionRegistries() {
    }
}

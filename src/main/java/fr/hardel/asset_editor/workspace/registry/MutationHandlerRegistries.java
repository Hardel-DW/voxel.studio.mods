package fr.hardel.asset_editor.workspace.registry;

import fr.hardel.asset_editor.workspace.registry.impl.EnchantmentMutationHandler;

public final class MutationHandlerRegistries {

    public static void register() {
        EnchantmentMutationHandler.register();
    }

    private MutationHandlerRegistries() {
    }
}

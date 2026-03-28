package fr.hardel.asset_editor.workspace.registry;

import fr.hardel.asset_editor.store.adapter.EnchantmentFlushAdapter;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.crafting.Recipe;

public final class WorkspaceBindings {

    public static void register() {
        RegistryWorkspaceBindings.register(new RegistryWorkspaceBinding<>(
            Registries.RECIPE,
            Recipe.CODEC,
            null,
            null));

        RegistryWorkspaceBindings.register(new RegistryWorkspaceBinding<>(
            Registries.ENCHANTMENT,
            Enchantment.DIRECT_CODEC,
            EnchantmentFlushAdapter.INSTANCE,
            entry -> EnchantmentFlushAdapter.initializeCustom(entry.data(), entry.tags())));
    }

    private WorkspaceBindings() {
    }
}

package fr.hardel.asset_editor.workspace.flush;

import fr.hardel.asset_editor.workspace.flush.adapter.EnchantmentFlushAdapter;
import fr.hardel.asset_editor.workspace.flush.adapter.RecipeFlushAdapter;
import fr.hardel.asset_editor.workspace.WorkspaceDefinition;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.enchantment.Enchantment;

public final class Workspaces {

    public static void register() {
        WorkspaceDefinition.register(Registries.ENCHANTMENT, Enchantment.DIRECT_CODEC,
            EnchantmentFlushAdapter.INSTANCE, entry -> EnchantmentFlushAdapter.initializeCustom(entry.data(), entry.tags()));
        WorkspaceDefinition.register(Registries.RECIPE, Recipe.CODEC,
            RecipeFlushAdapter.INSTANCE);
    }

    private Workspaces() {}
}

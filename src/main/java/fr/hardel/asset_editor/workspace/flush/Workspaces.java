package fr.hardel.asset_editor.workspace.flush;

import fr.hardel.asset_editor.workspace.flush.adapter.EnchantmentFlushAdapter;
import fr.hardel.asset_editor.workspace.flush.adapter.RecipeFlushAdapter;
import fr.hardel.asset_editor.workspace.WorkspaceDefinition;
import fr.hardel.asset_editor.workspace.WorkspaceDefinitions;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.enchantment.Enchantment;

public final class Workspaces {

    public static final WorkspaceDefinition<Enchantment> ENCHANTMENT = WorkspaceDefinition.of(
        Registries.ENCHANTMENT,
        Enchantment.DIRECT_CODEC,
        EnchantmentFlushAdapter.INSTANCE,
        entry -> EnchantmentFlushAdapter.initializeCustom(entry.data(), entry.tags()));

    public static final WorkspaceDefinition<Recipe<?>> RECIPE = WorkspaceDefinition.of(
        Registries.RECIPE,
        Recipe.CODEC,
        RecipeFlushAdapter.INSTANCE);

    public static void register() {
        WorkspaceDefinitions.register(ENCHANTMENT);
        WorkspaceDefinitions.register(RECIPE);
    }

    private Workspaces() {}
}

package fr.hardel.asset_editor.network.data;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.network.recipe.RecipeCatalogEntry;
import fr.hardel.asset_editor.data.compendium.CompendiumTagGroup;
import fr.hardel.asset_editor.data.recipe.RecipeEntryDefinition;
import net.minecraft.resources.Identifier;

public final class StudioDataKeys {

    public static final ServerDataKey<RecipeCatalogEntry> RECIPE_CATALOG = ServerDataKeys.register(
        ServerDataKey.of(Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "recipe_catalog"), RecipeCatalogEntry.STREAM_CODEC));

    public static final ServerDataKey<CompendiumTagGroup> COMPENDIUM_ITEMS = ServerDataKeys.register(
        ServerDataKey.of(Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "compendium_items"), CompendiumTagGroup.STREAM_CODEC));

    public static final ServerDataKey<CompendiumTagGroup> COMPENDIUM_ENCHANTMENTS = ServerDataKeys.register(
        ServerDataKey.of(Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "compendium_enchantments"), CompendiumTagGroup.STREAM_CODEC));

    public static final ServerDataKey<RecipeEntryDefinition> RECIPE_ENTRIES = ServerDataKeys.register(
        ServerDataKey.of(Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "recipe_entries"), RecipeEntryDefinition.STREAM_CODEC));

    public static void init() {
        // Force class loading to register all keys
    }

    private StudioDataKeys() {}
}

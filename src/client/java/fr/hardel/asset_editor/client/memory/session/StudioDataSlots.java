package fr.hardel.asset_editor.client.memory.session;

import fr.hardel.asset_editor.network.data.StudioDataKeys;
import fr.hardel.asset_editor.network.recipe.RecipeCatalogEntry;
import fr.hardel.asset_editor.studio.CompendiumTagGroup;
import fr.hardel.asset_editor.studio.RecipeEntryDefinition;

public final class StudioDataSlots {

    public static final ServerDataStore.DataSlot<RecipeCatalogEntry> RECIPE_CATALOG =
        ServerDataStore.register(StudioDataKeys.RECIPE_CATALOG);

    public static final ServerDataStore.DataSlot<CompendiumTagGroup> COMPENDIUM_ITEMS =
        ServerDataStore.register(StudioDataKeys.COMPENDIUM_ITEMS);

    public static final ServerDataStore.DataSlot<CompendiumTagGroup> COMPENDIUM_ENCHANTMENTS =
        ServerDataStore.register(StudioDataKeys.COMPENDIUM_ENCHANTMENTS);

    public static final ServerDataStore.DataSlot<RecipeEntryDefinition> RECIPE_ENTRIES =
        ServerDataStore.register(StudioDataKeys.RECIPE_ENTRIES);

    public static void init() {
        // Force class loading to register all slots
    }

    private StudioDataSlots() {}
}

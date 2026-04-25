package fr.hardel.asset_editor.client.memory.core;

import fr.hardel.asset_editor.network.data.StudioDataKeys;
import fr.hardel.asset_editor.network.recipe.RecipeCatalogEntry;
import fr.hardel.asset_editor.data.compendium.CompendiumTagGroup;
import fr.hardel.asset_editor.data.component.StudioComponentTypeDef;
import fr.hardel.asset_editor.data.recipe.RecipeEntryDefinition;
import fr.hardel.asset_editor.network.structure.StructureTemplateSnapshot;
import fr.hardel.asset_editor.network.structure.StructureWorldgenSnapshot;

public final class StudioDataSlots {

    public static final ServerDataStore.DataSlot<RecipeCatalogEntry> RECIPE_CATALOG =
        ServerDataStore.register(StudioDataKeys.RECIPE_CATALOG);

    public static final ServerDataStore.DataSlot<CompendiumTagGroup> COMPENDIUM_ITEMS =
        ServerDataStore.register(StudioDataKeys.COMPENDIUM_ITEMS);

    public static final ServerDataStore.DataSlot<CompendiumTagGroup> COMPENDIUM_ENCHANTMENTS =
        ServerDataStore.register(StudioDataKeys.COMPENDIUM_ENCHANTMENTS);

    public static final ServerDataStore.DataSlot<RecipeEntryDefinition> RECIPE_ENTRIES =
        ServerDataStore.register(StudioDataKeys.RECIPE_ENTRIES);

    public static final ServerDataStore.DataSlot<StudioComponentTypeDef> COMPONENT_TYPES =
        ServerDataStore.register(StudioDataKeys.COMPONENT_TYPES);

    public static final ServerDataStore.DataSlot<StructureTemplateSnapshot> STRUCTURE_TEMPLATES =
        ServerDataStore.register(StudioDataKeys.STRUCTURE_TEMPLATES);

    public static final ServerDataStore.DataSlot<StructureWorldgenSnapshot> STRUCTURE_WORLDGEN =
        ServerDataStore.register(StudioDataKeys.STRUCTURE_WORLDGEN);

    public static void register() {
        // Force class loading to register all slots
    }

    private StudioDataSlots() {}
}

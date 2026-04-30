package fr.hardel.asset_editor.network.data;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.data.compendium.CompendiumTagLoader;
import fr.hardel.asset_editor.data.recipe.RecipeEntryLoader;
import fr.hardel.asset_editor.network.registry.RegistryIdSnapshot;
import fr.hardel.asset_editor.network.registry.UnsyncedRegistryCatalog;
import fr.hardel.asset_editor.network.structure.StructureTemplateCatalog;
import fr.hardel.asset_editor.network.structure.StructureTemplateIndexEntry;
import fr.hardel.asset_editor.network.structure.StructureTemplateSnapshot;
import fr.hardel.asset_editor.network.structure.StructureWorldgenCatalog;
import fr.hardel.asset_editor.network.structure.StructureWorldgenSnapshot;
import fr.hardel.asset_editor.network.recipe.RecipeCatalogBuilder;
import fr.hardel.asset_editor.network.recipe.RecipeCatalogEntry;
import fr.hardel.asset_editor.data.compendium.CompendiumTagGroup;
import fr.hardel.asset_editor.data.recipe.RecipeEntryDefinition;
import net.minecraft.resources.Identifier;

public final class StudioDataKeys {

    public static final ServerDataKey<RecipeCatalogEntry> RECIPE_CATALOG = ServerDataKeys.register(
        ServerDataKey.of(Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "recipe_catalog"), RecipeCatalogEntry.STREAM_CODEC, RecipeCatalogBuilder::build));

    public static final ServerDataKey<CompendiumTagGroup> COMPENDIUM_ITEMS = ServerDataKeys.register(
        ServerDataKey.of(Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "compendium_items"), CompendiumTagGroup.STREAM_CODEC, server -> CompendiumTagLoader.itemGroups()));

    public static final ServerDataKey<CompendiumTagGroup> COMPENDIUM_ENCHANTMENTS = ServerDataKeys.register(
        ServerDataKey.of(Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "compendium_enchantments"), CompendiumTagGroup.STREAM_CODEC, server -> CompendiumTagLoader.enchantmentGroups()));

    public static final ServerDataKey<RecipeEntryDefinition> RECIPE_ENTRIES = ServerDataKeys.register(
        ServerDataKey.of(Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "recipe_entries"), RecipeEntryDefinition.STREAM_CODEC, server -> RecipeEntryLoader.entries()));

    public static final ServerDataKey<StructureTemplateIndexEntry> STRUCTURE_TEMPLATE_INDEX = ServerDataKeys.register(
        ServerDataKey.of(Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "structure_template_index"), StructureTemplateIndexEntry.STREAM_CODEC, StructureTemplateCatalog::buildIndex));

    public static final ServerDataKey<StructureTemplateSnapshot> STRUCTURE_TEMPLATES = ServerDataKeys.register(
        ServerDataKey.lazy(Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "structure_templates"), StructureTemplateSnapshot.STREAM_CODEC, StructureTemplateCatalog::build, StructureTemplateSnapshot::id, StructureTemplateCatalog::build));

    public static final ServerDataKey<StructureWorldgenSnapshot> STRUCTURE_WORLDGEN = ServerDataKeys.register(
        ServerDataKey.of(Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "structure_worldgen"), StructureWorldgenSnapshot.STREAM_CODEC, StructureWorldgenCatalog::build));

    public static final ServerDataKey<RegistryIdSnapshot> UNSYNCED_REGISTRY_IDS = ServerDataKeys.register(
        ServerDataKey.of(Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "unsynced_registry_ids"), RegistryIdSnapshot.STREAM_CODEC, UnsyncedRegistryCatalog::build));

    public static void init() {
        // Force class loading to register all keys
    }

    private StudioDataKeys() {}
}

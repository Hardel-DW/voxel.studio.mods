package fr.hardel.asset_editor.data;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.data.codec.CodecTypeLoader;
import fr.hardel.asset_editor.data.compendium.CompendiumTagLoader;
import fr.hardel.asset_editor.data.component.ComponentTypeLoader;
import fr.hardel.asset_editor.data.recipe.RecipeEntryLoader;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;

public final class StudioResourceLoaders {

    public static void register() {
        ResourceLoader.get(PackType.SERVER_DATA).registerReloader(
            Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "studio_compendium_tags"),
            new CompendiumTagLoader());
        ResourceLoader.get(PackType.SERVER_DATA).registerReloader(
            Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "studio_recipe_entries"),
            new RecipeEntryLoader());
        ResourceLoader.get(PackType.SERVER_DATA).registerReloader(
            Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "studio_codec"),
            new CodecTypeLoader());
        ResourceLoader.get(PackType.SERVER_DATA).registerReloader(
            Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "studio_codec_components"),
            new ComponentTypeLoader());
    }

    private StudioResourceLoaders() {}
}

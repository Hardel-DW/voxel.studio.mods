package fr.hardel.asset_editor.data;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.data.concept.StudioConceptDef;
import fr.hardel.asset_editor.data.concept.StudioEditorTabDef;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public final class StudioRegistries {

    public static final ResourceKey<Registry<StudioEditorTabDef>> STUDIO_TAB = ResourceKey.createRegistryKey(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "studio_tab"));

    public static final ResourceKey<Registry<StudioConceptDef>> STUDIO_CONCEPT = ResourceKey.createRegistryKey(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "studio_concept"));

    public static void register() {
        DynamicRegistries.registerSynced(STUDIO_TAB, StudioEditorTabDef.CODEC);
        DynamicRegistries.registerSynced(STUDIO_CONCEPT, StudioConceptDef.CODEC);
    }

    private StudioRegistries() {}
}

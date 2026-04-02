package fr.hardel.asset_editor;

import fr.hardel.asset_editor.command.AssetEditorCommands;
import fr.hardel.asset_editor.event.PackReloadEnd;
import fr.hardel.asset_editor.event.PlayerJoinEvent;
import fr.hardel.asset_editor.event.SeverStartedEvent;
import fr.hardel.asset_editor.event.ServerStoppedEvent;
import fr.hardel.asset_editor.network.AssetEditorNetworking;
import fr.hardel.asset_editor.studio.RecipeEntryLoader;
import fr.hardel.asset_editor.studio.SuggestedTagLoader;
import fr.hardel.asset_editor.workspace.action.EditorActionRegistries;
import fr.hardel.asset_editor.workspace.action.recipe.adapter.RecipeAdapterRegistries;
import fr.hardel.asset_editor.workspace.registry.MutationHandlerRegistries;
import fr.hardel.asset_editor.workspace.registry.WorkspaceBindings;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;

public class AssetEditor implements ModInitializer {

    public static final String MOD_ID = "asset_editor";
    public static final boolean DEV_DISABLE_SINGLEPLAYER_ADMIN = false;

    @Override
    public void onInitialize() {
        EditorActionRegistries.register();
        RecipeAdapterRegistries.register();
        MutationHandlerRegistries.register();
        WorkspaceBindings.register();
        AssetEditorNetworking.register();
        ResourceLoader.get(PackType.SERVER_DATA).registerReloader(
            Identifier.fromNamespaceAndPath(MOD_ID, "studio_suggested_tags"),
            new SuggestedTagLoader());
        ResourceLoader.get(PackType.SERVER_DATA).registerReloader(
            Identifier.fromNamespaceAndPath(MOD_ID, "studio_recipe_entries"),
            new RecipeEntryLoader());
        SeverStartedEvent.register();
        ServerStoppedEvent.register();
        PlayerJoinEvent.register();
        PackReloadEnd.register();
        AssetEditorCommands.register();
    }
}

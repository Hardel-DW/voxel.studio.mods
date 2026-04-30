package fr.hardel.asset_editor;

import fr.hardel.asset_editor.event.*;
import fr.hardel.asset_editor.network.AssetEditorNetworking;
import fr.hardel.asset_editor.data.StudioRegistries;
import fr.hardel.asset_editor.data.StudioResourceLoaders;
import fr.hardel.asset_editor.workspace.action.Actions;
import fr.hardel.asset_editor.workspace.action.recipe.adapter.RecipeAdapterRegistries;
import fr.hardel.asset_editor.workspace.flush.Workspaces;
import net.fabricmc.api.ModInitializer;

public class AssetEditor implements ModInitializer {

    public static final String MOD_ID = "asset_editor";
    public static final String STUDIO_NAMESPACE = "studio";

    @Override
    public void onInitialize() {
        StudioRegistries.register();
        RecipeAdapterRegistries.register();
        Workspaces.register();
        Actions.register();
        AssetEditorNetworking.register();
        StudioResourceLoaders.register();
        SeverStartedEvent.register();
        ServerStoppedEvent.register();
        PlayerJoinEvent.register();
        PackReloadEndEvent.register();
        CommandRegistrationEvent.register();
    }
}

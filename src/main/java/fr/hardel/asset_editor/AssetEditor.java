package fr.hardel.asset_editor;

import fr.hardel.asset_editor.command.AssetEditorCommands;
import fr.hardel.asset_editor.event.PackReloadEnd;
import fr.hardel.asset_editor.event.PlayerJoinEvent;
import fr.hardel.asset_editor.event.SeverStartedEvent;
import fr.hardel.asset_editor.event.ServerStoppedEvent;
import fr.hardel.asset_editor.network.AssetEditorNetworking;
import fr.hardel.asset_editor.workspace.action.EditorActionRegistries;
import fr.hardel.asset_editor.workspace.registry.MutationHandlerRegistries;
import fr.hardel.asset_editor.workspace.registry.WorkspaceBindings;
import net.fabricmc.api.ModInitializer;

public class AssetEditor implements ModInitializer {

    public static final String MOD_ID = "asset_editor";
    public static final boolean DEV_DISABLE_SINGLEPLAYER_ADMIN = false;

    @Override
    public void onInitialize() {
        EditorActionRegistries.register();
        MutationHandlerRegistries.register();
        WorkspaceBindings.register();
        AssetEditorNetworking.register();
        SeverStartedEvent.register();
        ServerStoppedEvent.register();
        PlayerJoinEvent.register();
        PackReloadEnd.register();
        AssetEditorCommands.register();
    }
}

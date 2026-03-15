package fr.hardel.asset_editor;

import fr.hardel.asset_editor.network.AssetEditorNetworking;
import fr.hardel.asset_editor.permission.PermissionManager;
import fr.hardel.asset_editor.permission.StudioPermissionCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class AssetEditor implements ModInitializer {

    public static final String MOD_ID = "asset_editor";
    public static final boolean DEV_DISABLE_SINGLEPLAYER_ADMIN = true;

    @Override
    public void onInitialize() {
        AssetEditorNetworking.registerServer();
        ServerLifecycleEvents.SERVER_STARTED.register(PermissionManager::init);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> PermissionManager.shutdown());
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> StudioPermissionCommand.register(dispatcher));
    }
}

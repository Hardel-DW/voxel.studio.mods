package fr.hardel.asset_editor.client;

import fr.hardel.asset_editor.client.compose.window.VoxelStudioWindow;
import fr.hardel.asset_editor.client.event.ClientTickHandler;
import fr.hardel.asset_editor.client.event.StudioKeybinding;
import fr.hardel.asset_editor.client.event.StudioReloadListener;
import fr.hardel.asset_editor.client.memory.session.server.StudioDataSlots;
import fr.hardel.asset_editor.client.network.ClientNetworkHandler;
import net.fabricmc.api.ClientModInitializer;

import static fr.hardel.asset_editor.client.compose.StudioRoutesKt.registerStudioRoutes;

public class AssetEditorClient implements ClientModInitializer {

    public static final String BUILD_VERSION = "26.0.1-RC";

    @Override
    public void onInitializeClient() {
        StudioDataSlots.register();
        ClientPreferences.register();
        StudioKeybinding.register();
        registerStudioRoutes();
        VoxelStudioWindow.initialize();
        ClientTickHandler.register();
        ClientNetworkHandler.register();
        StudioReloadListener.register();
    }
}

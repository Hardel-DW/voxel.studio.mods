package fr.hardel.asset_editor.client;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.client.bootstrap.ComposeBootstrap;
import fr.hardel.asset_editor.client.bootstrap.ui.ComposeDownloadHud;
import fr.hardel.asset_editor.client.event.ClientTickHandler;
import fr.hardel.asset_editor.client.event.StudioKeybinding;
import fr.hardel.asset_editor.client.event.StudioReloadListener;
import fr.hardel.asset_editor.client.memory.core.StudioDataSlots;
import fr.hardel.asset_editor.client.network.ClientNetworkHandler;
import net.fabricmc.api.ClientModInitializer;

public class AssetEditorClient implements ClientModInitializer {

    public static final String BUILD_VERSION = "26.0.1-RC";

    @Override
    public void onInitializeClient() {
        StudioDataSlots.register();
        ClientPreferences.register();
        StudioKeybinding.register();
        ClientTickHandler.register();
        ClientNetworkHandler.register();
        StudioReloadListener.register();
        ComposeDownloadHud.register();
        if (AssetEditor.DEV_CLEAR_COMPOSE_CACHE) ComposeBootstrap.purgeCache();
        ComposeBootstrap.tryLinkFromCache();
    }
}
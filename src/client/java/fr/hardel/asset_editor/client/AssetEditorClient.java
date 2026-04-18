package fr.hardel.asset_editor.client;

import fr.hardel.asset_editor.DevFlags;
import fr.hardel.asset_editor.client.bootstrap.ComposeBootstrap;
import fr.hardel.asset_editor.client.bootstrap.ui.ComposeDownloadHud;
import fr.hardel.asset_editor.client.compose.components.page.recipe.RecipeQuickSwap;
import fr.hardel.asset_editor.client.event.ClientTickHandler;
import fr.hardel.asset_editor.client.event.StudioKeybinding;
import fr.hardel.asset_editor.client.event.StudioReloadListener;
import fr.hardel.asset_editor.client.memory.core.StudioDataSlots;
import fr.hardel.asset_editor.client.network.ClientNetworkHandler;
import fr.hardel.asset_editor.client.splash.SplashAssets;
import net.fabricmc.api.ClientModInitializer;

public class AssetEditorClient implements ClientModInitializer {

    public static final String BUILD_VERSION = "26.0.1-RC";

    static {
        System.setProperty("java.awt.headless", "false");
        System.setProperty("compose.swing.render.on.graphics", "true");
        javax.swing.UIManager.put("Panel.background", java.awt.Color.BLACK);
    }

    @Override
    public void onInitializeClient() {
        StudioDataSlots.register();
        ClientPreferences.register();
        StudioKeybinding.register();
        ClientTickHandler.register();
        ClientNetworkHandler.register();
        StudioReloadListener.register();
        RecipeQuickSwap.INSTANCE.registerDefaults();
        ComposeDownloadHud.register();
        if (DevFlags.CLEAR_COMPOSE_CACHE) ComposeBootstrap.purgeCache();
        ComposeBootstrap.tryLinkFromCache();
        SplashAssets.preloadAsync();
    }
}

package fr.hardel.asset_editor.event;

import fr.hardel.asset_editor.network.AssetEditorNetworking;
import fr.hardel.asset_editor.store.ServerPackService;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public final class PackReloadEnd {

    private static final ServerPackService PACK_SERVICE = new ServerPackService();

    public static void register() {
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (!success)
                return;

            AssetEditorNetworking.broadcastRecipeCatalog(server);
            AssetEditorNetworking.broadcastStudioConfig(server);
            PACK_SERVICE.listPacks().ifPresent(packs -> AssetEditorNetworking.broadcastPackList(server, packs));
        });
    }

    private PackReloadEnd() {
    }
}

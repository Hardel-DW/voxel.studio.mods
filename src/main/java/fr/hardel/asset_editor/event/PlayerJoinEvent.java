package fr.hardel.asset_editor.event;

import fr.hardel.asset_editor.network.AssetEditorNetworking;
import fr.hardel.asset_editor.permission.PermissionManager;
import fr.hardel.asset_editor.store.ServerPackService;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public final class PlayerJoinEvent {

    private static final ServerPackService PACK_SERVICE = new ServerPackService();

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            server.execute(() -> {
                var permissionManager = PermissionManager.get();
                if (permissionManager != null) {
                    permissionManager.syncToPlayer(handler.getPlayer());
                }

                AssetEditorNetworking.sendAllServerData(handler.getPlayer(), server);
                PACK_SERVICE.listPacks()
                    .ifPresent(packs -> AssetEditorNetworking.sendPackList(handler.getPlayer(), packs));
            });
        });
    }

    private PlayerJoinEvent() {
    }
}

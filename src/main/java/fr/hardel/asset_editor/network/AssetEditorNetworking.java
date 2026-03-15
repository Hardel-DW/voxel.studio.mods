package fr.hardel.asset_editor.network;

import fr.hardel.asset_editor.permission.PermissionManager;
import fr.hardel.asset_editor.permission.StudioPermissions;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public final class AssetEditorNetworking {

    public static void registerServer() {
        PayloadTypeRegistry.playS2C().register(PermissionSyncPayload.TYPE, PermissionSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(EditorActionResponsePayload.TYPE, EditorActionResponsePayload.CODEC);

        PayloadTypeRegistry.playC2S().register(PermissionRequestPayload.TYPE, PermissionRequestPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(PermissionRequestPayload.TYPE, AssetEditorNetworking::handlePermissionRequest);

        PayloadTypeRegistry.playC2S().register(EditorActionPayload.TYPE, EditorActionPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(EditorActionPayload.TYPE, AssetEditorNetworking::handleEditorAction);
    }

    public static void sendPermissions(ServerPlayer player, StudioPermissions permissions) {
        ServerPlayNetworking.send(player, new PermissionSyncPayload(permissions));
    }

    private static void handlePermissionRequest(PermissionRequestPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            var manager = PermissionManager.get();
            if (manager == null) return;
            manager.syncToPlayer(context.player());
        });
    }

    private static void handleEditorAction(EditorActionPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer player = context.player();
            var manager = PermissionManager.get();
            if (manager == null) {
                sendResponse(player, payload.actionId(), false, "error:server_unavailable");
                return;
            }

            var perms = manager.getEffectivePermissions(player);
            if (!perms.canEditElement(payload.registryId(), payload.targetId())) {
                sendResponse(player, payload.actionId(), false, "error:permission_denied");
                return;
            }

            sendResponse(player, payload.actionId(), true, "");
        });
    }

    private static void sendResponse(ServerPlayer player, java.util.UUID actionId, boolean accepted, String message) {
        ServerPlayNetworking.send(player, new EditorActionResponsePayload(actionId, accepted, message));
    }

    private AssetEditorNetworking() {
    }
}

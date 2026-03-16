package fr.hardel.asset_editor.client.network;

import fr.hardel.asset_editor.client.javafx.VoxelStudioWindow;
import fr.hardel.asset_editor.network.EditorActionResponsePayload;
import fr.hardel.asset_editor.network.ElementUpdatePayload;
import fr.hardel.asset_editor.network.PermissionSyncPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ClientNetworkHandler {

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(PermissionSyncPayload.TYPE, (payload, context) ->
                context.client().execute(() -> VoxelStudioWindow.updatePermissions(payload.permissions())));

        ClientPlayNetworking.registerGlobalReceiver(EditorActionResponsePayload.TYPE, (payload, context) ->
                context.client().execute(() -> VoxelStudioWindow.handleActionResponse(
                        payload.actionId(), payload.accepted(), payload.message())));

        ClientPlayNetworking.registerGlobalReceiver(ElementUpdatePayload.TYPE, (payload, context) ->
                context.client().execute(() -> VoxelStudioWindow.handleElementUpdate(
                        payload.registryId(), payload.targetId(), payload.action())));
    }

    private ClientNetworkHandler() {
    }
}

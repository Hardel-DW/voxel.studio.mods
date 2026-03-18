package fr.hardel.asset_editor.client.network;

import fr.hardel.asset_editor.client.AssetEditorClient;
import fr.hardel.asset_editor.network.EditorActionResponsePayload;
import fr.hardel.asset_editor.network.ElementUpdatePayload;
import fr.hardel.asset_editor.network.PackListSyncPayload;
import fr.hardel.asset_editor.network.PermissionSyncPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ClientNetworkHandler {

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(PermissionSyncPayload.TYPE, (payload, context) ->
            context.client().execute(() -> AssetEditorClient.sessionDispatch().handlePermissionSync(payload.permissions())));

        ClientPlayNetworking.registerGlobalReceiver(PackListSyncPayload.TYPE, (payload, context) ->
            context.client().execute(() -> AssetEditorClient.sessionDispatch().handlePackListSync(payload.packs())));

        ClientPlayNetworking.registerGlobalReceiver(EditorActionResponsePayload.TYPE, (payload, context) ->
            context.client().execute(() -> AssetEditorClient.sessionDispatch().handleActionResponse(
                payload.actionId(), payload.accepted(), payload.message())));

        ClientPlayNetworking.registerGlobalReceiver(ElementUpdatePayload.TYPE, (payload, context) ->
            context.client().execute(() -> AssetEditorClient.sessionDispatch().handleElementUpdate(
                payload.registryId(), payload.targetId(), payload.action())));
    }

    private ClientNetworkHandler() {
    }
}

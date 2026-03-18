package fr.hardel.asset_editor.client.network;

import fr.hardel.asset_editor.client.AssetEditorClient;
import fr.hardel.asset_editor.network.pack.PackListSyncPayload;
import fr.hardel.asset_editor.network.pack.PackWorkspaceSyncPayload;
import fr.hardel.asset_editor.network.session.PermissionSyncPayload;
import fr.hardel.asset_editor.network.workspace.WorkspaceSyncPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ClientNetworkHandler {

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(PermissionSyncPayload.TYPE, (payload, context) ->
            context.client().execute(() -> AssetEditorClient.sessionDispatch().handlePermissionSync(payload.permissions())));

        ClientPlayNetworking.registerGlobalReceiver(PackListSyncPayload.TYPE, (payload, context) ->
            context.client().execute(() -> AssetEditorClient.sessionDispatch().handlePackListSync(payload.packs())));

        ClientPlayNetworking.registerGlobalReceiver(WorkspaceSyncPayload.TYPE, (payload, context) ->
            context.client().execute(() -> AssetEditorClient.sessionDispatch().handleWorkspaceSync(payload)));

        ClientPlayNetworking.registerGlobalReceiver(PackWorkspaceSyncPayload.TYPE, (payload, context) ->
            context.client().execute(() -> AssetEditorClient.sessionDispatch().handlePackWorkspaceSync(payload)));
    }

    private ClientNetworkHandler() {
    }
}

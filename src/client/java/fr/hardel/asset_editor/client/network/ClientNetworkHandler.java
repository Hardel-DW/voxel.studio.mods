package fr.hardel.asset_editor.client.network;

import fr.hardel.asset_editor.client.AssetEditorClient;
import fr.hardel.asset_editor.network.pack.PackListSyncPayload;
import fr.hardel.asset_editor.network.pack.PackWorkspaceSyncPayload;
import fr.hardel.asset_editor.network.recipe.RecipeCatalogSyncPayload;
import fr.hardel.asset_editor.network.session.PermissionSyncPayload;
import fr.hardel.asset_editor.network.workspace.WorkspaceSyncPayload;
import fr.hardel.asset_editor.client.memory.debug.NetworkTraceMemory;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class ClientNetworkHandler {

    public static void register() {
        registerInbound(PermissionSyncPayload.TYPE, (payload, context) -> context.client().execute(() -> AssetEditorClient.sessionDispatch().handlePermissionSync(payload)));
        registerInbound(PackListSyncPayload.TYPE, (payload, context) -> context.client().execute(() -> AssetEditorClient.sessionDispatch().handlePackListSync(payload)));
        registerInbound(RecipeCatalogSyncPayload.TYPE, (payload, context) -> context.client().execute(() -> AssetEditorClient.sessionDispatch().handleRecipeCatalogSync(payload)));
        registerInbound(WorkspaceSyncPayload.TYPE, (payload, context) -> context.client().execute(() -> AssetEditorClient.sessionDispatch().handleWorkspaceSync(payload)));
        registerInbound(PackWorkspaceSyncPayload.TYPE, (payload, context) -> context.client().execute(() -> AssetEditorClient.sessionDispatch().handlePackWorkspaceSync(payload)));
    }

    private static <T extends CustomPacketPayload> void registerInbound(CustomPacketPayload.Type<T> type, ClientPlayNetworking.PlayPayloadHandler<T> handler) {
        ClientPlayNetworking.registerGlobalReceiver(type, (payload, context) -> {
            AssetEditorClient.debugMemory().network().capture(NetworkTraceMemory.Direction.INBOUND, payload);
            handler.receive(payload, context);
        });
    }

    private ClientNetworkHandler() {}
}

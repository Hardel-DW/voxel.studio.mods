package fr.hardel.asset_editor.client.network;

import fr.hardel.asset_editor.client.compose.components.page.structure.StructureAssemblyMemory;
import fr.hardel.asset_editor.client.memory.ClientMemoryHolder;
import fr.hardel.asset_editor.network.data.ServerDataSyncPayload;
import fr.hardel.asset_editor.network.pack.PackListSyncPayload;
import fr.hardel.asset_editor.network.pack.PackWorkspaceSyncPayload;
import fr.hardel.asset_editor.network.PermissionSyncPayload;
import fr.hardel.asset_editor.network.structure.StructureAssemblyResponsePayload;
import fr.hardel.asset_editor.network.workspace.WorkspaceSyncPayload;
import fr.hardel.asset_editor.client.memory.session.debug.NetworkTraceMemory;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class ClientNetworkHandler {

    public static void register() {
        registerInbound(PermissionSyncPayload.TYPE, (payload, context) -> context.client().execute(() -> ClientMemoryHolder.dispatch().handlePermissionSync(payload)));
        registerInbound(PackListSyncPayload.TYPE, (payload, context) -> context.client().execute(() -> ClientMemoryHolder.dispatch().handlePackListSync(payload)));
        registerInbound(WorkspaceSyncPayload.TYPE, (payload, context) -> context.client().execute(() -> ClientMemoryHolder.dispatch().handleWorkspaceSync(payload)));
        registerInbound(PackWorkspaceSyncPayload.TYPE, (payload, context) -> context.client().execute(() -> ClientMemoryHolder.dispatch().handlePackWorkspaceSync(payload)));
        registerInbound(ServerDataSyncPayload.TYPE, (payload, context) -> context.client().execute(() -> ClientMemoryHolder.dispatch().handleServerDataSync(payload)));
        registerInbound(StructureAssemblyResponsePayload.TYPE, (payload, context) -> context.client().execute(() -> StructureAssemblyMemory.receiveResponse(payload)));
    }

    private static <T extends CustomPacketPayload> void registerInbound(CustomPacketPayload.Type<T> type, ClientPlayNetworking.PlayPayloadHandler<T> handler) {
        ClientPlayNetworking.registerGlobalReceiver(type, (payload, context) -> {
            ClientMemoryHolder.debug().network().capture(NetworkTraceMemory.Direction.INBOUND, payload);
            handler.receive(payload, context);
        });
    }

    private ClientNetworkHandler() {}
}

package fr.hardel.asset_editor.client.network;

import fr.hardel.asset_editor.client.AssetEditorClient;
import fr.hardel.asset_editor.client.memory.debug.NetworkTraceMemory;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class ClientPayloadSender {

    public static void send(CustomPacketPayload payload) {
        AssetEditorClient.debugMemory().network().capture(NetworkTraceMemory.Direction.OUTBOUND, payload);
        ClientPlayNetworking.send(payload);
    }

    private ClientPayloadSender() {
    }
}

package fr.hardel.asset_editor.client.network;

import fr.hardel.asset_editor.client.debug.NetworkTraceStore;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class ClientPayloadSender {

    public static void send(CustomPacketPayload payload) {
        NetworkTraceStore.capture(NetworkTraceStore.Direction.OUTBOUND, payload);
        ClientPlayNetworking.send(payload);
    }

    private ClientPayloadSender() {
    }
}

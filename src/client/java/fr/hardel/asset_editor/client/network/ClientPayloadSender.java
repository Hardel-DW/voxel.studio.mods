package fr.hardel.asset_editor.client.network;

import fr.hardel.asset_editor.client.memory.ClientMemoryHolder;
import fr.hardel.asset_editor.client.memory.session.debug.NetworkTraceMemory;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class ClientPayloadSender {

    public static void send(CustomPacketPayload payload) {
        ClientMemoryHolder.debug().network().capture(NetworkTraceMemory.Direction.OUTBOUND, payload);
        ClientPlayNetworking.send(payload);
    }

    private ClientPayloadSender() {
    }
}

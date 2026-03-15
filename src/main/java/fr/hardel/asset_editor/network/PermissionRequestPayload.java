package fr.hardel.asset_editor.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record PermissionRequestPayload() implements CustomPacketPayload {

    public static final Type<PermissionRequestPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("asset_editor", "permission_request"));

    public static final StreamCodec<ByteBuf, PermissionRequestPayload> CODEC =
            StreamCodec.unit(new PermissionRequestPayload());

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

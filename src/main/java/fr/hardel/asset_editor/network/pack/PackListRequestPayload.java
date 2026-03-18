package fr.hardel.asset_editor.network.pack;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record PackListRequestPayload() implements CustomPacketPayload {

    public static final Type<PackListRequestPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath("asset_editor", "pack_list_request"));

    public static final StreamCodec<ByteBuf, PackListRequestPayload> CODEC = StreamCodec.unit(new PackListRequestPayload());

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

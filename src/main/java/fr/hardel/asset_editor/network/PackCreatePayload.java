package fr.hardel.asset_editor.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record PackCreatePayload(String name, String namespace) implements CustomPacketPayload {

    public static final Type<PackCreatePayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath("asset_editor", "pack_create"));

    public static final StreamCodec<ByteBuf, PackCreatePayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, PackCreatePayload::name,
        ByteBufCodecs.STRING_UTF8, PackCreatePayload::namespace,
        PackCreatePayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

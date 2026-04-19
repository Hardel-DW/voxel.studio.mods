package fr.hardel.asset_editor.network.pack;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import fr.hardel.asset_editor.AssetEditor;

public record PackCreatePayload(String name, String namespace, byte[] icon) implements CustomPacketPayload {

    public static final int MAX_ICON_BYTES = 512 * 1024;

    public static final Type<PackCreatePayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "pack_create"));

    public static final StreamCodec<ByteBuf, PackCreatePayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, PackCreatePayload::name,
        ByteBufCodecs.STRING_UTF8, PackCreatePayload::namespace,
        ByteBufCodecs.byteArray(MAX_ICON_BYTES), PackCreatePayload::icon,
        PackCreatePayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package fr.hardel.asset_editor.network.pack;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record PackWorkspaceRequestPayload(
    String packId,
    Identifier registryId) implements CustomPacketPayload {

    public static final Type<PackWorkspaceRequestPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath("asset_editor", "pack_workspace_request"));

    public static final StreamCodec<ByteBuf, PackWorkspaceRequestPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, PackWorkspaceRequestPayload::packId,
        Identifier.STREAM_CODEC, PackWorkspaceRequestPayload::registryId,
        PackWorkspaceRequestPayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

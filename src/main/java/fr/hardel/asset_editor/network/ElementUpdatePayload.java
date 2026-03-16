package fr.hardel.asset_editor.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record ElementUpdatePayload(
        Identifier registryId,
        Identifier targetId,
        EditorAction action
) implements CustomPacketPayload {

    public static final Type<ElementUpdatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("asset_editor", "element_update"));

    public static final StreamCodec<ByteBuf, ElementUpdatePayload> CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, ElementUpdatePayload::registryId,
            Identifier.STREAM_CODEC, ElementUpdatePayload::targetId,
            EditorAction.STREAM_CODEC, ElementUpdatePayload::action,
            ElementUpdatePayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

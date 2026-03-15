package fr.hardel.asset_editor.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record EditorActionResponsePayload(
        UUID actionId,
        boolean accepted,
        String message
) implements CustomPacketPayload {

    public static final Type<EditorActionResponsePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("asset_editor", "editor_action_response"));

    private static final StreamCodec<ByteBuf, UUID> UUID_CODEC = StreamCodec.of(
            (buf, uuid) -> { buf.writeLong(uuid.getMostSignificantBits()); buf.writeLong(uuid.getLeastSignificantBits()); },
            buf -> new UUID(buf.readLong(), buf.readLong()));

    public static final StreamCodec<ByteBuf, EditorActionResponsePayload> CODEC = StreamCodec.composite(
            UUID_CODEC, EditorActionResponsePayload::actionId,
            ByteBufCodecs.BOOL, EditorActionResponsePayload::accepted,
            ByteBufCodecs.STRING_UTF8, EditorActionResponsePayload::message,
            EditorActionResponsePayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

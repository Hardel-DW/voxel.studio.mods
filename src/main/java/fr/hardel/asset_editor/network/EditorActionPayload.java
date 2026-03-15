package fr.hardel.asset_editor.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record EditorActionPayload(
        UUID actionId,
        Identifier registryId,
        Identifier targetId,
        EditorAction action) implements CustomPacketPayload {

        public static final Type<EditorActionPayload> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath("asset_editor", "editor_action"));

        private static final StreamCodec<ByteBuf, UUID> UUID_CODEC = StreamCodec.of(
                (buf, uuid) -> {
                        buf.writeLong(uuid.getMostSignificantBits());
                        buf.writeLong(uuid.getLeastSignificantBits());
                },
                buf -> new UUID(buf.readLong(), buf.readLong()));

        public static final StreamCodec<ByteBuf, EditorActionPayload> CODEC = StreamCodec.composite(
                UUID_CODEC, EditorActionPayload::actionId,
                Identifier.STREAM_CODEC, EditorActionPayload::registryId,
                Identifier.STREAM_CODEC, EditorActionPayload::targetId,
                EditorAction.STREAM_CODEC, EditorActionPayload::action,
                EditorActionPayload::new);

        @Override
        public @NotNull Type<? extends CustomPacketPayload> type() {
                return TYPE;
        }
}

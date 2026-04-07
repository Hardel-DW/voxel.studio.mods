package fr.hardel.asset_editor.network.workspace;

import io.netty.buffer.ByteBuf;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import fr.hardel.asset_editor.AssetEditor;

import java.util.UUID;

public record WorkspaceMutationRequestPayload(UUID actionId, String packId, Identifier registryId, Identifier targetId, EditorAction<?> action) implements CustomPacketPayload {

    public static final Type<WorkspaceMutationRequestPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "workspace_mutation_request"));

    private static final StreamCodec<ByteBuf, UUID> UUID_CODEC = StreamCodec.of(
        (buf, uuid) -> {
            buf.writeLong(uuid.getMostSignificantBits());
            buf.writeLong(uuid.getLeastSignificantBits());
        },
        buf -> new UUID(buf.readLong(), buf.readLong()));

    public static final StreamCodec<ByteBuf, WorkspaceMutationRequestPayload> CODEC = StreamCodec.composite(
        UUID_CODEC, WorkspaceMutationRequestPayload::actionId,
        ByteBufCodecs.STRING_UTF8, WorkspaceMutationRequestPayload::packId,
        Identifier.STREAM_CODEC, WorkspaceMutationRequestPayload::registryId,
        Identifier.STREAM_CODEC, WorkspaceMutationRequestPayload::targetId,
        EditorAction.STREAM_CODEC, WorkspaceMutationRequestPayload::action,
        WorkspaceMutationRequestPayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

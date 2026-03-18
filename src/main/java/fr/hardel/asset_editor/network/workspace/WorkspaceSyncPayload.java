package fr.hardel.asset_editor.network.workspace;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record WorkspaceSyncPayload(
    UUID actionId,
    String packId,
    boolean mutationResponse,
    boolean accepted,
    String errorCode,
    WorkspaceElementSnapshot snapshot) implements CustomPacketPayload {

    public WorkspaceSyncPayload {
        packId = packId == null ? "" : packId;
        errorCode = errorCode == null ? "" : errorCode;
    }

    public static final Type<WorkspaceSyncPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath("asset_editor", "workspace_sync"));

    private static final StreamCodec<ByteBuf, UUID> UUID_CODEC = StreamCodec.of(
        (buf, uuid) -> {
            buf.writeLong(uuid.getMostSignificantBits());
            buf.writeLong(uuid.getLeastSignificantBits());
        },
        buf -> new UUID(buf.readLong(), buf.readLong()));

    public static final StreamCodec<ByteBuf, WorkspaceSyncPayload> CODEC = StreamCodec.of(
        (buf, payload) -> {
            ByteBufCodecs.BOOL.encode(buf, payload.actionId() != null);
            if (payload.actionId() != null)
                UUID_CODEC.encode(buf, payload.actionId());
            ByteBufCodecs.STRING_UTF8.encode(buf, payload.packId());
            ByteBufCodecs.BOOL.encode(buf, payload.mutationResponse());
            ByteBufCodecs.BOOL.encode(buf, payload.accepted());
            ByteBufCodecs.STRING_UTF8.encode(buf, payload.errorCode());
            ByteBufCodecs.BOOL.encode(buf, payload.snapshot() != null);
            if (payload.snapshot() != null)
                WorkspaceElementSnapshot.STREAM_CODEC.encode(buf, payload.snapshot());
        },
        buf -> {
            UUID actionId = ByteBufCodecs.BOOL.decode(buf) ? UUID_CODEC.decode(buf) : null;
            String packId = ByteBufCodecs.STRING_UTF8.decode(buf);
            boolean mutationResponse = ByteBufCodecs.BOOL.decode(buf);
            boolean accepted = ByteBufCodecs.BOOL.decode(buf);
            String errorCode = ByteBufCodecs.STRING_UTF8.decode(buf);
            WorkspaceElementSnapshot snapshot = ByteBufCodecs.BOOL.decode(buf)
                ? WorkspaceElementSnapshot.STREAM_CODEC.decode(buf)
                : null;
            return new WorkspaceSyncPayload(actionId, packId, mutationResponse, accepted, errorCode, snapshot);
        });

    public static WorkspaceSyncPayload mutationResult(UUID actionId, String packId, boolean accepted,
        String errorCode, WorkspaceElementSnapshot snapshot) {
        return new WorkspaceSyncPayload(actionId, packId, true, accepted, errorCode, snapshot);
    }

    public static WorkspaceSyncPayload remoteSync(String packId, WorkspaceElementSnapshot snapshot) {
        return new WorkspaceSyncPayload(null, packId, false, true, "", snapshot);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

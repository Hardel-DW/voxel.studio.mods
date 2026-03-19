package fr.hardel.asset_editor.network.pack;

import fr.hardel.asset_editor.network.workspace.WorkspaceElementSnapshot;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record PackWorkspaceSyncPayload(String packId, Identifier registryId, List<WorkspaceElementSnapshot> entries) implements CustomPacketPayload {

    public PackWorkspaceSyncPayload {
        entries = List.copyOf(entries == null ? List.of() : entries);
    }

    public static final Type<PackWorkspaceSyncPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath("asset_editor", "pack_workspace_sync"));

    public static final StreamCodec<ByteBuf, PackWorkspaceSyncPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, PackWorkspaceSyncPayload::packId,
        Identifier.STREAM_CODEC, PackWorkspaceSyncPayload::registryId,
        WorkspaceElementSnapshot.STREAM_CODEC.apply(ByteBufCodecs.list()), PackWorkspaceSyncPayload::entries,
        PackWorkspaceSyncPayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

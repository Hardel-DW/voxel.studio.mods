package fr.hardel.asset_editor.network.pack;

import fr.hardel.asset_editor.network.workspace.WorkspaceElementSnapshot;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import fr.hardel.asset_editor.AssetEditor;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;

public record PackWorkspaceSyncPayload(String packId, Identifier registryId,
    List<WorkspaceElementSnapshot> entries, Set<Identifier> modifiedIds) implements CustomPacketPayload {

    public PackWorkspaceSyncPayload {
        entries = List.copyOf(entries == null ? List.of() : entries);
        modifiedIds = Set.copyOf(modifiedIds == null ? Set.of() : modifiedIds);
    }

    public static final Type<PackWorkspaceSyncPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "pack_workspace_sync"));

    private static final StreamCodec<ByteBuf, Set<Identifier>> MODIFIED_IDS_CODEC =
        ByteBufCodecs.collection((IntFunction<Set<Identifier>>) LinkedHashSet::new, Identifier.STREAM_CODEC);

    public static final StreamCodec<ByteBuf, PackWorkspaceSyncPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, PackWorkspaceSyncPayload::packId,
        Identifier.STREAM_CODEC, PackWorkspaceSyncPayload::registryId,
        WorkspaceElementSnapshot.STREAM_CODEC.apply(ByteBufCodecs.list()), PackWorkspaceSyncPayload::entries,
        MODIFIED_IDS_CODEC, PackWorkspaceSyncPayload::modifiedIds,
        PackWorkspaceSyncPayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

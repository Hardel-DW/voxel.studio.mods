package fr.hardel.asset_editor.network.pack;

import fr.hardel.asset_editor.store.ServerPackManager.PackEntry;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record PackListSyncPayload(List<PackEntry> packs) implements CustomPacketPayload {

    public static final Type<PackListSyncPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath("asset_editor", "pack_list_sync"));

    private static final StreamCodec<ByteBuf, PackEntry> ENTRY_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, PackEntry::packId,
        ByteBufCodecs.STRING_UTF8, PackEntry::name,
        ByteBufCodecs.BOOL, PackEntry::writable,
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), PackEntry::namespaces,
        PackEntry::new);

    public static final StreamCodec<ByteBuf, PackListSyncPayload> CODEC = ENTRY_CODEC.apply(ByteBufCodecs.list()).map(PackListSyncPayload::new, PackListSyncPayload::packs);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

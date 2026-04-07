package fr.hardel.asset_editor.network.data;

import fr.hardel.asset_editor.AssetEditor;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record ServerDataSyncPayload(Identifier key, byte[] rawData) implements CustomPacketPayload {

    public static final Type<ServerDataSyncPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "server_data_sync"));

    public static final StreamCodec<ByteBuf, ServerDataSyncPayload> CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, ServerDataSyncPayload::key,
        ByteBufCodecs.BYTE_ARRAY, ServerDataSyncPayload::rawData,
        ServerDataSyncPayload::new
    );

    public static <T> ServerDataSyncPayload create(ServerDataKey<T> key, java.util.List<T> data) {
        return new ServerDataSyncPayload(key.id(), key.encode(data));
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package fr.hardel.asset_editor.network.data;

import fr.hardel.asset_editor.AssetEditor;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record ServerDataRequestPayload(Identifier key) implements CustomPacketPayload {

    public static final Type<ServerDataRequestPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "server_data_request"));

    public static final StreamCodec<ByteBuf, ServerDataRequestPayload> CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, ServerDataRequestPayload::key,
        ServerDataRequestPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

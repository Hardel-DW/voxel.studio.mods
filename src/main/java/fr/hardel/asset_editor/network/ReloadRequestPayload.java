package fr.hardel.asset_editor.network;

import fr.hardel.asset_editor.AssetEditor;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record ReloadRequestPayload() implements CustomPacketPayload {

    public static final Type<ReloadRequestPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "reload_request"));

    public static final StreamCodec<ByteBuf, ReloadRequestPayload> CODEC = StreamCodec.unit(new ReloadRequestPayload());

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

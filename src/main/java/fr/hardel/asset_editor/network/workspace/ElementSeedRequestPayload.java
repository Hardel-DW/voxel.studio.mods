package fr.hardel.asset_editor.network.workspace;

import fr.hardel.asset_editor.AssetEditor;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record ElementSeedRequestPayload(String packId, Identifier registryId, Identifier elementId) implements CustomPacketPayload {

    public static final Type<ElementSeedRequestPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "element_seed_request"));

    public static final StreamCodec<ByteBuf, ElementSeedRequestPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, ElementSeedRequestPayload::packId,
        Identifier.STREAM_CODEC, ElementSeedRequestPayload::registryId,
        Identifier.STREAM_CODEC, ElementSeedRequestPayload::elementId,
        ElementSeedRequestPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

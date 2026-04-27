package fr.hardel.asset_editor.network.structure;

import fr.hardel.asset_editor.AssetEditor;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record StructureLocateRequestPayload(Identifier structureId) implements CustomPacketPayload {
    public static final Type<StructureLocateRequestPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "structure_locate_request"));

    public static final StreamCodec<ByteBuf, StructureLocateRequestPayload> CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, StructureLocateRequestPayload::structureId,
        StructureLocateRequestPayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

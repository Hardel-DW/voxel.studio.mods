package fr.hardel.asset_editor.network.structure;

import fr.hardel.asset_editor.AssetEditor;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record StructureReplaceBlocksPayload(String packId, Identifier structureId, Identifier fromBlock, Identifier toBlock) implements CustomPacketPayload {
    public StructureReplaceBlocksPayload {
        packId = packId == null ? "" : packId;
    }

    public static final Type<StructureReplaceBlocksPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "structure_replace_blocks"));

    public static final StreamCodec<ByteBuf, StructureReplaceBlocksPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, StructureReplaceBlocksPayload::packId,
        Identifier.STREAM_CODEC, StructureReplaceBlocksPayload::structureId,
        Identifier.STREAM_CODEC, StructureReplaceBlocksPayload::fromBlock,
        Identifier.STREAM_CODEC, StructureReplaceBlocksPayload::toBlock,
        StructureReplaceBlocksPayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

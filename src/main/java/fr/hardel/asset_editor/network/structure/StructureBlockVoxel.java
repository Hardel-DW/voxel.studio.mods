package fr.hardel.asset_editor.network.structure;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * @param blockStateId Vanilla {@code Block.BLOCK_STATE_REGISTRY} id — preserves every property
 *                     of the source palette entry. Reconstruct via {@code Block.stateById(int)}.
 *                     {@code blockId} is kept alongside as a fast path for filters / counts /
 *                     jigsaw detection that don't need the full state.
 */
public record StructureBlockVoxel(Identifier blockId, int blockStateId, int x, int y, int z) {
    public static final StreamCodec<ByteBuf, StructureBlockVoxel> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, StructureBlockVoxel::blockId,
        ByteBufCodecs.VAR_INT, StructureBlockVoxel::blockStateId,
        ByteBufCodecs.VAR_INT, StructureBlockVoxel::x,
        ByteBufCodecs.VAR_INT, StructureBlockVoxel::y,
        ByteBufCodecs.VAR_INT, StructureBlockVoxel::z,
        StructureBlockVoxel::new);
}

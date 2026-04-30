package fr.hardel.asset_editor.network.structure;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public record StructureBlockVoxel(Identifier blockId, int blockStateId, int x, int y, int z, int finalStateId) {
    public static final StreamCodec<ByteBuf, StructureBlockVoxel> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, StructureBlockVoxel::blockId,
        ByteBufCodecs.VAR_INT, StructureBlockVoxel::blockStateId,
        ByteBufCodecs.VAR_INT, StructureBlockVoxel::x,
        ByteBufCodecs.VAR_INT, StructureBlockVoxel::y,
        ByteBufCodecs.VAR_INT, StructureBlockVoxel::z,
        ByteBufCodecs.VAR_INT, StructureBlockVoxel::finalStateId,
        StructureBlockVoxel::new);
}

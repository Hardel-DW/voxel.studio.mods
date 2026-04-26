package fr.hardel.asset_editor.network.structure;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public record StructureAssemblyVoxel(Identifier blockId, int blockStateId, int x, int y, int z, int pieceIndex, int finalStateId) {
    public static final StreamCodec<ByteBuf, StructureAssemblyVoxel> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, StructureAssemblyVoxel::blockId,
        ByteBufCodecs.VAR_INT, StructureAssemblyVoxel::blockStateId,
        ByteBufCodecs.VAR_INT, StructureAssemblyVoxel::x,
        ByteBufCodecs.VAR_INT, StructureAssemblyVoxel::y,
        ByteBufCodecs.VAR_INT, StructureAssemblyVoxel::z,
        ByteBufCodecs.VAR_INT, StructureAssemblyVoxel::pieceIndex,
        ByteBufCodecs.VAR_INT, StructureAssemblyVoxel::finalStateId,
        StructureAssemblyVoxel::new);
}

package fr.hardel.asset_editor.network.structure;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.List;

public record StructureAssemblySnapshot(
    Identifier id,
    int sizeX,
    int sizeY,
    int sizeZ,
    int pieceCount,
    List<StructureAssemblyVoxel> voxels
) {
    public StructureAssemblySnapshot {
        voxels = List.copyOf(voxels == null ? List.of() : voxels);
    }

    public static final StreamCodec<ByteBuf, StructureAssemblySnapshot> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, StructureAssemblySnapshot::id,
        ByteBufCodecs.VAR_INT, StructureAssemblySnapshot::sizeX,
        ByteBufCodecs.VAR_INT, StructureAssemblySnapshot::sizeY,
        ByteBufCodecs.VAR_INT, StructureAssemblySnapshot::sizeZ,
        ByteBufCodecs.VAR_INT, StructureAssemblySnapshot::pieceCount,
        StructureAssemblyVoxel.STREAM_CODEC.apply(ByteBufCodecs.list()), StructureAssemblySnapshot::voxels,
        StructureAssemblySnapshot::new);
}

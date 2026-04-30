package fr.hardel.asset_editor.network.structure;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.List;

public record StructureTemplateSnapshot(
    Identifier id,
    String sourcePack,
    int sizeX,
    int sizeY,
    int sizeZ,
    int totalBlocks,
    int entityCount,
    List<StructureBlockCount> blockCounts,
    List<StructureBlockVoxel> voxels,
    List<StructureJigsawNode> jigsaws
) {
    public StructureTemplateSnapshot {
        sourcePack = sourcePack == null ? "" : sourcePack;
        blockCounts = List.copyOf(blockCounts == null ? List.of() : blockCounts);
        voxels = List.copyOf(voxels == null ? List.of() : voxels);
        jigsaws = List.copyOf(jigsaws == null ? List.of() : jigsaws);
    }

    public static final StreamCodec<ByteBuf, StructureTemplateSnapshot> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, StructureTemplateSnapshot::id,
        ByteBufCodecs.STRING_UTF8, StructureTemplateSnapshot::sourcePack,
        ByteBufCodecs.VAR_INT, StructureTemplateSnapshot::sizeX,
        ByteBufCodecs.VAR_INT, StructureTemplateSnapshot::sizeY,
        ByteBufCodecs.VAR_INT, StructureTemplateSnapshot::sizeZ,
        ByteBufCodecs.VAR_INT, StructureTemplateSnapshot::totalBlocks,
        ByteBufCodecs.VAR_INT, StructureTemplateSnapshot::entityCount,
        StructureBlockCount.STREAM_CODEC.apply(ByteBufCodecs.list()), StructureTemplateSnapshot::blockCounts,
        StructureBlockVoxel.STREAM_CODEC.apply(ByteBufCodecs.list()), StructureTemplateSnapshot::voxels,
        StructureJigsawNode.STREAM_CODEC.apply(ByteBufCodecs.list()), StructureTemplateSnapshot::jigsaws,
        StructureTemplateSnapshot::new);
}

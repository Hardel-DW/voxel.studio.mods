package fr.hardel.asset_editor.network.structure;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public record StructureBlockVoxel(Identifier blockId, String state, int x, int y, int z) {
    public static final StreamCodec<ByteBuf, StructureBlockVoxel> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, StructureBlockVoxel::blockId,
        ByteBufCodecs.STRING_UTF8, StructureBlockVoxel::state,
        ByteBufCodecs.VAR_INT, StructureBlockVoxel::x,
        ByteBufCodecs.VAR_INT, StructureBlockVoxel::y,
        ByteBufCodecs.VAR_INT, StructureBlockVoxel::z,
        StructureBlockVoxel::new);
}

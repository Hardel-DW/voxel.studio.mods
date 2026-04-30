package fr.hardel.asset_editor.network.structure;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public record StructureBlockCount(Identifier blockId, int count) {
    public static final StreamCodec<ByteBuf, StructureBlockCount> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, StructureBlockCount::blockId,
        ByteBufCodecs.VAR_INT, StructureBlockCount::count,
        StructureBlockCount::new);
}

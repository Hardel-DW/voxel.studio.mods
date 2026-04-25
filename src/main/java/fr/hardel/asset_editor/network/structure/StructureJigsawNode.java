package fr.hardel.asset_editor.network.structure;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record StructureJigsawNode(int x, int y, int z, String name, String target, String pool, String finalState) {
    public StructureJigsawNode {
        name = name == null ? "" : name;
        target = target == null ? "" : target;
        pool = pool == null ? "" : pool;
        finalState = finalState == null ? "" : finalState;
    }

    public static final StreamCodec<ByteBuf, StructureJigsawNode> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, StructureJigsawNode::x,
        ByteBufCodecs.VAR_INT, StructureJigsawNode::y,
        ByteBufCodecs.VAR_INT, StructureJigsawNode::z,
        ByteBufCodecs.STRING_UTF8, StructureJigsawNode::name,
        ByteBufCodecs.STRING_UTF8, StructureJigsawNode::target,
        ByteBufCodecs.STRING_UTF8, StructureJigsawNode::pool,
        ByteBufCodecs.STRING_UTF8, StructureJigsawNode::finalState,
        StructureJigsawNode::new);
}

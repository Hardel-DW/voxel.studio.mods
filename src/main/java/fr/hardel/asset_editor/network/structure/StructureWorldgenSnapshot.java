package fr.hardel.asset_editor.network.structure;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public record StructureWorldgenSnapshot(
    Identifier id,
    String sourcePack,
    String type,
    String startPool,
    int size,
    int maxDistanceFromCenter,
    String iconPath
) {
    public StructureWorldgenSnapshot {
        sourcePack = sourcePack == null ? "" : sourcePack;
        type = type == null ? "" : type;
        startPool = startPool == null ? "" : startPool;
        iconPath = iconPath == null ? "" : iconPath;
    }

    public static final StreamCodec<ByteBuf, StructureWorldgenSnapshot> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, StructureWorldgenSnapshot::id,
        ByteBufCodecs.STRING_UTF8, StructureWorldgenSnapshot::sourcePack,
        ByteBufCodecs.STRING_UTF8, StructureWorldgenSnapshot::type,
        ByteBufCodecs.STRING_UTF8, StructureWorldgenSnapshot::startPool,
        ByteBufCodecs.VAR_INT, StructureWorldgenSnapshot::size,
        ByteBufCodecs.VAR_INT, StructureWorldgenSnapshot::maxDistanceFromCenter,
        ByteBufCodecs.STRING_UTF8, StructureWorldgenSnapshot::iconPath,
        StructureWorldgenSnapshot::new);
}

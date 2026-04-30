package fr.hardel.asset_editor.network.structure;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public record StructureTemplateIndexEntry(
    Identifier id,
    String sourcePack
) {
    public StructureTemplateIndexEntry {
        sourcePack = sourcePack == null ? "" : sourcePack;
    }

    public static final StreamCodec<ByteBuf, StructureTemplateIndexEntry> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, StructureTemplateIndexEntry::id,
        ByteBufCodecs.STRING_UTF8, StructureTemplateIndexEntry::sourcePack,
        StructureTemplateIndexEntry::new);
}

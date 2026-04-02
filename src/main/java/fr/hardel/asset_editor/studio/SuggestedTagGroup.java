package fr.hardel.asset_editor.studio;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.List;

public record SuggestedTagGroup(Identifier id, List<SuggestedTagEntry> entries) {

    public static final StreamCodec<ByteBuf, SuggestedTagGroup> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, SuggestedTagGroup::id,
        SuggestedTagEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), SuggestedTagGroup::entries,
        SuggestedTagGroup::new
    );

    public SuggestedTagGroup {
        entries = List.copyOf(entries == null ? List.of() : entries);
    }
}

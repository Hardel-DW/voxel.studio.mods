package fr.hardel.asset_editor.tag;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

public record TagSeed(List<TagSeedEntry> values, List<TagSeedEntry> exclude, boolean replace) {

    public static final Codec<TagSeed> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        TagSeedEntry.CODEC.listOf().optionalFieldOf("values", List.of()).forGetter(TagSeed::values),
        TagSeedEntry.CODEC.listOf().optionalFieldOf("exclude", List.of()).forGetter(TagSeed::exclude),
        Codec.BOOL.optionalFieldOf("replace", false).forGetter(TagSeed::replace)
    ).apply(instance, TagSeed::new));

    public static final StreamCodec<ByteBuf, TagSeed> STREAM_CODEC = StreamCodec.composite(
        TagSeedEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), TagSeed::values,
        TagSeedEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), TagSeed::exclude,
        ByteBufCodecs.BOOL, TagSeed::replace,
        TagSeed::new
    );

    public TagSeed {
        values = List.copyOf(values == null ? List.of() : values);
        exclude = List.copyOf(exclude == null ? List.of() : exclude);
    }

    public ExtendedTagFile toTagFile() {
        return new ExtendedTagFile(
            values.stream().map(TagSeedEntry::toTagEntry).toList(),
            exclude.stream().map(TagSeedEntry::toTagEntry).toList(),
            replace
        );
    }

    public static TagSeed fromValueLiterals(List<String> values) {
        return new TagSeed(
            (values == null ? List.<String>of() : values).stream().map(TagSeedEntry::fromLiteral).toList(),
            List.of(),
            false
        );
    }
}

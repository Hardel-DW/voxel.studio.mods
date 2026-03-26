package fr.hardel.asset_editor.tag;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagEntry;

public record TagSeedEntry(Identifier id, boolean tag, boolean required) {

    public static final Codec<TagSeedEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Identifier.CODEC.fieldOf("id").forGetter(TagSeedEntry::id),
        Codec.BOOL.optionalFieldOf("tag", false).forGetter(TagSeedEntry::tag),
        Codec.BOOL.optionalFieldOf("required", true).forGetter(TagSeedEntry::required)
    ).apply(instance, TagSeedEntry::new));

    public static final StreamCodec<ByteBuf, TagSeedEntry> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, TagSeedEntry::id,
        ByteBufCodecs.BOOL, TagSeedEntry::tag,
        ByteBufCodecs.BOOL, TagSeedEntry::required,
        TagSeedEntry::new
    );

    public TagSeedEntry {
        if (id == null)
            throw new IllegalArgumentException("Tag seed entry id is required");
    }

    public TagEntry toTagEntry() {
        if (tag)
            return required ? TagEntry.tag(id) : TagEntry.optionalTag(id);
        return required ? TagEntry.element(id) : TagEntry.optionalElement(id);
    }

    public static TagSeedEntry fromLiteral(String raw) {
        if (raw == null || raw.isBlank())
            throw new IllegalArgumentException("Tag seed literal is blank");

        boolean required = !raw.endsWith("?");
        String normalized = required ? raw : raw.substring(0, raw.length() - 1);
        boolean tag = normalized.startsWith("#");
        Identifier id = Identifier.tryParse(tag ? normalized.substring(1) : normalized);
        if (id == null)
            throw new IllegalArgumentException("Invalid tag seed literal: " + raw);

        return new TagSeedEntry(id, tag, required);
    }
}

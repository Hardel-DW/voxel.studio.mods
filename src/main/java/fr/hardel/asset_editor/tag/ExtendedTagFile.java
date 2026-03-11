package fr.hardel.asset_editor.tag;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.tags.TagEntry;

import java.util.List;

public record ExtendedTagFile(List<TagEntry> entries, List<TagEntry> exclude, boolean replace) {
    public static final Codec<ExtendedTagFile> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    TagEntry.CODEC.listOf().optionalFieldOf("values", List.of()).forGetter(ExtendedTagFile::entries),
                    TagEntry.CODEC.listOf().optionalFieldOf("exclude", List.of()).forGetter(ExtendedTagFile::exclude),
                    Codec.BOOL.optionalFieldOf("replace", false).forGetter(ExtendedTagFile::replace))
                    .apply(instance, ExtendedTagFile::new));
}

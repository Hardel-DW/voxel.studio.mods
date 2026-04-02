package fr.hardel.asset_editor.studio;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.List;

public record SuggestedTagEntry(Identifier id, List<String> defaults) {

    private static final Codec<SuggestedTagEntry> OBJECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Identifier.CODEC.fieldOf("id").forGetter(SuggestedTagEntry::id),
        Codec.STRING.listOf().optionalFieldOf("default", List.of()).forGetter(SuggestedTagEntry::defaults)
    ).apply(instance, SuggestedTagEntry::new));

    private static final Codec<SuggestedTagEntry> STRING_CODEC =
        Identifier.CODEC.xmap(id -> new SuggestedTagEntry(id, List.of()), SuggestedTagEntry::id);

    public static final Codec<SuggestedTagEntry> CODEC =
        Codec.either(STRING_CODEC, OBJECT_CODEC)
            .xmap(either -> either.map(e -> e, e -> e), Either::right);

    public static final StreamCodec<ByteBuf, SuggestedTagEntry> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, SuggestedTagEntry::id,
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), SuggestedTagEntry::defaults,
        SuggestedTagEntry::new
    );

    public SuggestedTagEntry {
        defaults = List.copyOf(defaults == null ? List.of() : defaults);
    }

    public boolean hasDefaults() {
        return !defaults.isEmpty();
    }
}

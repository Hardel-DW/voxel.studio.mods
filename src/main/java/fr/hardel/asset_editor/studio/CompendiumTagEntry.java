package fr.hardel.asset_editor.studio;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.List;

public record CompendiumTagEntry(Identifier id, List<String> defaults) {

    private static final Codec<CompendiumTagEntry> OBJECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Identifier.CODEC.fieldOf("id").forGetter(CompendiumTagEntry::id),
        Codec.STRING.listOf().optionalFieldOf("default", List.of()).forGetter(CompendiumTagEntry::defaults)
    ).apply(instance, CompendiumTagEntry::new));

    private static final Codec<CompendiumTagEntry> STRING_CODEC =
        Identifier.CODEC.xmap(id -> new CompendiumTagEntry(id, List.of()), CompendiumTagEntry::id);

    public static final Codec<CompendiumTagEntry> CODEC =
        Codec.either(STRING_CODEC, OBJECT_CODEC)
            .xmap(either -> either.map(e -> e, e -> e), Either::right);

    public static final StreamCodec<ByteBuf, CompendiumTagEntry> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, CompendiumTagEntry::id,
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), CompendiumTagEntry::defaults,
        CompendiumTagEntry::new
    );

    public CompendiumTagEntry {
        defaults = List.copyOf(defaults == null ? List.of() : defaults);
    }

    public boolean hasDefaults() {
        return !defaults.isEmpty();
    }
}

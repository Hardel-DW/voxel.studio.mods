package fr.hardel.asset_editor.network.recipe;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record RecipeCatalogEntry(Identifier id, String type, Map<String, List<String>> slots, String resultItemId, int resultCount) {

    private static final StreamCodec<ByteBuf, Map<String, List<String>>> SLOTS_CODEC =
        ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()));

    public static final StreamCodec<ByteBuf, RecipeCatalogEntry> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, RecipeCatalogEntry::id,
        ByteBufCodecs.STRING_UTF8, RecipeCatalogEntry::type,
        SLOTS_CODEC, RecipeCatalogEntry::slots,
        ByteBufCodecs.STRING_UTF8, RecipeCatalogEntry::resultItemId,
        ByteBufCodecs.VAR_INT, RecipeCatalogEntry::resultCount,
        RecipeCatalogEntry::new
    );
}

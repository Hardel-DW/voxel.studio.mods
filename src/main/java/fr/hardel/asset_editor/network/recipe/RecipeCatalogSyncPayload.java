package fr.hardel.asset_editor.network.recipe;

import fr.hardel.asset_editor.AssetEditor;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record RecipeCatalogSyncPayload(List<Entry> entries) implements CustomPacketPayload {

    public static final Type<RecipeCatalogSyncPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "recipe_catalog_sync"));

    private static final StreamCodec<ByteBuf, Entry> ENTRY_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, Entry::id,
        ByteBufCodecs.STRING_UTF8, Entry::type,
        Entry::new
    );

    public static final StreamCodec<ByteBuf, RecipeCatalogSyncPayload> CODEC =
        ENTRY_CODEC.apply(ByteBufCodecs.list()).map(RecipeCatalogSyncPayload::new, RecipeCatalogSyncPayload::entries);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Entry(Identifier id, String type) {
    }
}

package fr.hardel.asset_editor.network.studio;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.studio.RecipeEntryDefinition;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record RecipeEntrySyncPayload(List<RecipeEntryDefinition> entries) implements CustomPacketPayload {

    public static final Type<RecipeEntrySyncPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "recipe_entry_sync"));

    public static final StreamCodec<ByteBuf, RecipeEntrySyncPayload> CODEC =
        RecipeEntryDefinition.STREAM_CODEC
            .apply(ByteBufCodecs.list())
            .map(RecipeEntrySyncPayload::new, RecipeEntrySyncPayload::entries);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

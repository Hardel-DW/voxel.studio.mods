package fr.hardel.asset_editor.network.studio;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.studio.SuggestedTagGroup;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record SuggestedExclusiveSyncPayload(List<SuggestedTagGroup> groups) implements CustomPacketPayload {

    public static final Type<SuggestedExclusiveSyncPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "suggested_enchantment_tag_sync"));

    public static final StreamCodec<ByteBuf, SuggestedExclusiveSyncPayload> CODEC =
        SuggestedTagGroup.STREAM_CODEC
            .apply(ByteBufCodecs.list())
            .map(SuggestedExclusiveSyncPayload::new, SuggestedExclusiveSyncPayload::groups);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

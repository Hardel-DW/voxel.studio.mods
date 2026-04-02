package fr.hardel.asset_editor.network.studio;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.studio.CompendiumTagGroup;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record CompendiumEnchantmentSyncPayload(List<CompendiumTagGroup> groups) implements CustomPacketPayload {

    public static final Type<CompendiumEnchantmentSyncPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "compendium_enchantment_tag_sync"));

    public static final StreamCodec<ByteBuf, CompendiumEnchantmentSyncPayload> CODEC =
        CompendiumTagGroup.STREAM_CODEC
            .apply(ByteBufCodecs.list())
            .map(CompendiumEnchantmentSyncPayload::new, CompendiumEnchantmentSyncPayload::groups);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

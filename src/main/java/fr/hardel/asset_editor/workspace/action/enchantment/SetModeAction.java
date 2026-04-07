package fr.hardel.asset_editor.workspace.action.enchantment;

import fr.hardel.asset_editor.workspace.flush.ElementEntry;
import fr.hardel.asset_editor.workspace.io.RegistryMutationContext;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import fr.hardel.asset_editor.workspace.flush.adapter.EnchantmentFlushAdapter.EnchantmentMode;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.enchantment.Enchantment;

public record SetModeAction(String mode) implements EditorAction<Enchantment> {

    public static final StreamCodec<ByteBuf, SetModeAction> CODEC =
        StreamCodec.composite(ByteBufCodecs.STRING_UTF8, SetModeAction::mode, SetModeAction::new);

    @Override
    public ElementEntry<Enchantment> apply(ElementEntry<Enchantment> entry, RegistryMutationContext ctx) {
        EnchantmentMode normalized = EnchantmentMode.fromId(mode);
        return entry.withCustom(entry.custom().with(EnchantmentMode.CUSTOM_FIELD_KEY, normalized.id()));
    }
}

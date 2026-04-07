package fr.hardel.asset_editor.workspace.action.enchantment;

import fr.hardel.asset_editor.workspace.flush.ElementEntry;
import fr.hardel.asset_editor.workspace.io.RegistryMutationContext;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import fr.hardel.asset_editor.workspace.flush.adapter.EnchantmentFlushAdapter.EnchantmentMode;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.enchantment.Enchantment;

public record ToggleDisabledAction() implements EditorAction<Enchantment> {

    public static final StreamCodec<ByteBuf, ToggleDisabledAction> CODEC = StreamCodec.unit(new ToggleDisabledAction());

    @Override
    public ElementEntry<Enchantment> apply(ElementEntry<Enchantment> entry, RegistryMutationContext ctx) {
        EnchantmentMode current = EnchantmentMode.fromId(entry.custom().getString(EnchantmentMode.CUSTOM_FIELD_KEY, EnchantmentMode.NORMAL.id()));
        EnchantmentMode next = current == EnchantmentMode.DISABLE ? EnchantmentMode.NORMAL : EnchantmentMode.DISABLE;

        return entry.withCustom(entry.custom().with(EnchantmentMode.CUSTOM_FIELD_KEY, next.id()));
    }
}

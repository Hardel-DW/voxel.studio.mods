package fr.hardel.asset_editor.workspace.action.enchantment;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import fr.hardel.asset_editor.workspace.action.EditorActionType;
import fr.hardel.asset_editor.workspace.flush.EnchantmentFlushAdapter.EnchantmentMode;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;

public record ToggleDisabledAction() implements EditorAction {

    public static final EditorActionType<Enchantment, ToggleDisabledAction> TYPE = new EditorActionType<>(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "enchantment/toggle_disabled"),
        ToggleDisabledAction.class,
        StreamCodec.unit(new ToggleDisabledAction()),
        (entry, action, ctx) -> {
            EnchantmentMode current = EnchantmentMode.fromId(entry.custom().getString(EnchantmentMode.CUSTOM_FIELD_KEY, EnchantmentMode.NORMAL.id()));
            EnchantmentMode next = current == EnchantmentMode.DISABLE ? EnchantmentMode.NORMAL : EnchantmentMode.DISABLE;
            return entry.withCustom(entry.custom().with(EnchantmentMode.CUSTOM_FIELD_KEY, next.id()));
        });

    @Override
    public EditorActionType<Enchantment, ToggleDisabledAction> type() {
        return TYPE;
    }
}

package fr.hardel.asset_editor.workspace.action.enchantment;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import fr.hardel.asset_editor.workspace.action.EditorActionType;
import fr.hardel.asset_editor.workspace.flush.EnchantmentFlushAdapter.EnchantmentMode;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;

public record SetModeAction(String mode) implements EditorAction {

    public static final EditorActionType<Enchantment, SetModeAction> TYPE = new EditorActionType<>(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "enchantment/set_mode"),
        SetModeAction.class,
        StreamCodec.composite(ByteBufCodecs.STRING_UTF8, SetModeAction::mode, SetModeAction::new),
        (entry, action, ctx) -> {
            EnchantmentMode normalized = EnchantmentMode.fromId(action.mode());
            return entry.withCustom(entry.custom().with(EnchantmentMode.CUSTOM_FIELD_KEY, normalized.id()));
        });

    @Override
    public EditorActionType<Enchantment, SetModeAction> type() {
        return TYPE;
    }
}

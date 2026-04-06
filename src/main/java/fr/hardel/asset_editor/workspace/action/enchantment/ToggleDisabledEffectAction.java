package fr.hardel.asset_editor.workspace.action.enchantment;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import fr.hardel.asset_editor.workspace.action.EditorActionType;
import fr.hardel.asset_editor.workspace.flush.EnchantmentFlushAdapter;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.LinkedHashSet;
import java.util.Set;

public record ToggleDisabledEffectAction(String effectId) implements EditorAction {

    public static final EditorActionType<Enchantment, ToggleDisabledEffectAction> TYPE = new EditorActionType<>(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "enchantment/toggle_disabled_effect"),
        ToggleDisabledEffectAction.class,
        StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ToggleDisabledEffectAction::effectId, ToggleDisabledEffectAction::new),
        (entry, action, ctx) -> {
            Set<String> next = new LinkedHashSet<>(entry.custom().getStringSet(EnchantmentFlushAdapter.DISABLED_EFFECTS_KEY));
            if (!next.remove(action.effectId()))
                next.add(action.effectId());
            return entry.withCustom(entry.custom().with(EnchantmentFlushAdapter.DISABLED_EFFECTS_KEY, next));
        });

    @Override
    public EditorActionType<Enchantment, ToggleDisabledEffectAction> type() {
        return TYPE;
    }
}

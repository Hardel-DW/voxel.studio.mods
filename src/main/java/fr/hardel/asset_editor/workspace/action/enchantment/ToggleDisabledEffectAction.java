package fr.hardel.asset_editor.workspace.action.enchantment;

import fr.hardel.asset_editor.workspace.flush.ElementEntry;
import fr.hardel.asset_editor.workspace.io.RegistryMutationContext;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import fr.hardel.asset_editor.workspace.flush.adapter.EnchantmentFlushAdapter;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.LinkedHashSet;
import java.util.Set;

public record ToggleDisabledEffectAction(String effectId) implements EditorAction<Enchantment> {

    public static final StreamCodec<ByteBuf, ToggleDisabledEffectAction> CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ToggleDisabledEffectAction::effectId, ToggleDisabledEffectAction::new);

    @Override
    public ElementEntry<Enchantment> apply(ElementEntry<Enchantment> entry, RegistryMutationContext ctx) {
        Set<String> next = new LinkedHashSet<>(entry.custom().getStringSet(EnchantmentFlushAdapter.DISABLED_EFFECTS_KEY));
        if (!next.remove(effectId))
            next.add(effectId);

        return entry.withCustom(entry.custom().with(EnchantmentFlushAdapter.DISABLED_EFFECTS_KEY, next));
    }
}

package fr.hardel.asset_editor.workspace.action.enchantment;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.workspace.ElementEntry;
import fr.hardel.asset_editor.workspace.RegistryMutationContext;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import fr.hardel.asset_editor.workspace.action.EditorActionType;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;

public record ToggleExclusiveAction(Identifier enchantmentId) implements EditorAction {

    public static final EditorActionType<Enchantment, ToggleExclusiveAction> TYPE = new EditorActionType<>(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "enchantment/toggle_exclusive"),
        ToggleExclusiveAction.class,
        StreamCodec.composite(Identifier.STREAM_CODEC, ToggleExclusiveAction::enchantmentId, ToggleExclusiveAction::new),
        (entry, action, ctx) -> apply(entry, action.enchantmentId(), ctx));

    @Override
    public EditorActionType<Enchantment, ToggleExclusiveAction> type() {
        return TYPE;
    }

    private static ElementEntry<Enchantment> apply(ElementEntry<Enchantment> entry, Identifier enchantmentId, RegistryMutationContext ctx) {
        var lookup = ctx.registries().lookupOrThrow(Registries.ENCHANTMENT);
        var holder = lookup.get(ResourceKey.create(Registries.ENCHANTMENT, enchantmentId)).orElse(null);
        if (holder == null)
            return entry;

        Enchantment e = entry.data();
        if (e.exclusiveSet().unwrapKey().isPresent()) {
            return entry.withData(new Enchantment(e.description(), e.definition(), HolderSet.direct(List.of(holder)), e.effects()));
        }

        List<Holder<Enchantment>> current = new ArrayList<>(e.exclusiveSet().stream().toList());
        boolean removed = current.removeIf(h -> h.unwrapKey().map(k -> k.identifier().equals(enchantmentId)).orElse(false));
        if (!removed)
            current.add(holder);

        HolderSet<Enchantment> newSet = current.isEmpty() ? HolderSet.empty() : HolderSet.direct(current);
        return entry.withData(new Enchantment(e.description(), e.definition(), newSet, e.effects()));
    }
}

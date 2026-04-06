package fr.hardel.asset_editor.workspace.action.enchantment;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import fr.hardel.asset_editor.workspace.action.EditorActionType;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;

public record SetExclusiveSetAction(String tagId) implements EditorAction {

    public static final EditorActionType<Enchantment, SetExclusiveSetAction> TYPE = new EditorActionType<>(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "enchantment/set_exclusive_set"),
        SetExclusiveSetAction.class,
        StreamCodec.composite(ByteBufCodecs.STRING_UTF8, SetExclusiveSetAction::tagId, SetExclusiveSetAction::new),
        (entry, action, ctx) -> {
            Enchantment e = entry.data();
            if (action.tagId().isEmpty())
                return entry.withData(new Enchantment(e.description(), e.definition(), HolderSet.empty(), e.effects()));

            Identifier id = Identifier.tryParse(action.tagId());
            if (id == null)
                return entry;

            HolderSet<Enchantment> resolved = ctx.resolveTagReference(Registries.ENCHANTMENT, id);
            return entry.withData(new Enchantment(e.description(), e.definition(), resolved == null ? HolderSet.empty() : resolved, e.effects()));
        });

    @Override
    public EditorActionType<Enchantment, SetExclusiveSetAction> type() {
        return TYPE;
    }
}

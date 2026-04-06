package fr.hardel.asset_editor.workspace.action.enchantment;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import fr.hardel.asset_editor.workspace.action.EditorActionType;
import fr.hardel.asset_editor.SlotManager;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantment.EnchantmentDefinition;

import java.util.List;

public record ToggleSlotAction(String slot) implements EditorAction {

    public static final EditorActionType<Enchantment, ToggleSlotAction> TYPE = new EditorActionType<>(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "enchantment/toggle_slot"),
        ToggleSlotAction.class,
        StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ToggleSlotAction::slot, ToggleSlotAction::new),
        (entry, action, ctx) -> {
            EquipmentSlotGroup group = EquipmentSlotGroup.valueOf(action.slot().toUpperCase());
            List<EquipmentSlotGroup> slots = new SlotManager(entry.data().definition().slots()).toggle(group).toGroups();
            Enchantment e = entry.data();
            EnchantmentDefinition d = e.definition();
            return entry.withData(new Enchantment(e.description(),
                new EnchantmentDefinition(d.supportedItems(), d.primaryItems(), d.weight(), d.maxLevel(), d.minCost(), d.maxCost(), d.anvilCost(), slots),
                e.exclusiveSet(), e.effects()));
        });

    @Override
    public EditorActionType<Enchantment, ToggleSlotAction> type() {
        return TYPE;
    }
}

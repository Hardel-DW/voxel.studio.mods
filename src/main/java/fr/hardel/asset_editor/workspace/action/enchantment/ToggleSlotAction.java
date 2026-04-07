package fr.hardel.asset_editor.workspace.action.enchantment;

import fr.hardel.asset_editor.SlotManager;
import fr.hardel.asset_editor.workspace.ElementEntry;
import fr.hardel.asset_editor.workspace.RegistryMutationContext;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantment.EnchantmentDefinition;

import java.util.List;

public record ToggleSlotAction(String slot) implements EditorAction<Enchantment> {

    public static final StreamCodec<ByteBuf, ToggleSlotAction> CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ToggleSlotAction::slot, ToggleSlotAction::new);

    @Override
    public ElementEntry<Enchantment> apply(ElementEntry<Enchantment> entry, RegistryMutationContext ctx) {
        EquipmentSlotGroup group = EquipmentSlotGroup.valueOf(slot.toUpperCase());
        List<EquipmentSlotGroup> slots = new SlotManager(entry.data().definition().slots()).toggle(group).toGroups();
        Enchantment e = entry.data();
        EnchantmentDefinition d = e.definition();

        return entry.withData(new Enchantment(e.description(),
            new EnchantmentDefinition(d.supportedItems(), d.primaryItems(), d.weight(), d.maxLevel(), d.minCost(), d.maxCost(), d.anvilCost(), slots),
            e.exclusiveSet(), e.effects()));
    }
}

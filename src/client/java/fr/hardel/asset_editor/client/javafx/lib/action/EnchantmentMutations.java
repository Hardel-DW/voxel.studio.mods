package fr.hardel.asset_editor.client.javafx.lib.action;

import fr.hardel.asset_editor.client.javafx.lib.SlotManager;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantment.EnchantmentDefinition;

import java.util.function.UnaryOperator;

public final class EnchantmentMutations {

    public static Enchantment withDefinition(Enchantment e, UnaryOperator<EnchantmentDefinition> transform) {
        return new Enchantment(e.description(), transform.apply(e.definition()), e.exclusiveSet(), e.effects());
    }

    public static Enchantment toggleSlot(Enchantment e, EquipmentSlotGroup slot) {
        var slots = new SlotManager(e.definition().slots()).toggle(slot).toGroups();
        return withDefinition(e, d -> new EnchantmentDefinition(
                d.supportedItems(), d.primaryItems(), d.weight(), d.maxLevel(),
                d.minCost(), d.maxCost(), d.anvilCost(), slots));
    }

    private EnchantmentMutations() {}
}

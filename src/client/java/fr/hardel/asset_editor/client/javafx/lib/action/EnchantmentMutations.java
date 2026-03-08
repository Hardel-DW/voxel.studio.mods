package fr.hardel.asset_editor.client.javafx.lib.action;

import fr.hardel.asset_editor.client.javafx.lib.SlotManager;
import net.minecraft.core.HolderSet;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantment.Cost;
import net.minecraft.world.item.enchantment.Enchantment.EnchantmentDefinition;

import java.util.Optional;
import java.util.function.UnaryOperator;

public final class EnchantmentMutations {

    public static Enchantment withDefinition(Enchantment e, UnaryOperator<EnchantmentDefinition> transform) {
        return new Enchantment(e.description(), transform.apply(e.definition()), e.exclusiveSet(), e.effects());
    }

    public static UnaryOperator<Enchantment> maxLevel(int value) {
        return e -> withDefinition(e, d -> new EnchantmentDefinition(
                d.supportedItems(), d.primaryItems(), d.weight(), value,
                d.minCost(), d.maxCost(), d.anvilCost(), d.slots()));
    }

    public static UnaryOperator<Enchantment> weight(int value) {
        return e -> withDefinition(e, d -> new EnchantmentDefinition(
                d.supportedItems(), d.primaryItems(), value, d.maxLevel(),
                d.minCost(), d.maxCost(), d.anvilCost(), d.slots()));
    }

    public static UnaryOperator<Enchantment> anvilCost(int value) {
        return e -> withDefinition(e, d -> new EnchantmentDefinition(
                d.supportedItems(), d.primaryItems(), d.weight(), d.maxLevel(),
                d.minCost(), d.maxCost(), value, d.slots()));
    }

    public static UnaryOperator<Enchantment> minCostBase(int value) {
        return e -> withDefinition(e, d -> new EnchantmentDefinition(
                d.supportedItems(), d.primaryItems(), d.weight(), d.maxLevel(),
                new Cost(value, d.minCost().perLevelAboveFirst()), d.maxCost(), d.anvilCost(), d.slots()));
    }

    public static UnaryOperator<Enchantment> minCostPerLevel(int value) {
        return e -> withDefinition(e, d -> new EnchantmentDefinition(
                d.supportedItems(), d.primaryItems(), d.weight(), d.maxLevel(),
                new Cost(d.minCost().base(), value), d.maxCost(), d.anvilCost(), d.slots()));
    }

    public static UnaryOperator<Enchantment> maxCostBase(int value) {
        return e -> withDefinition(e, d -> new EnchantmentDefinition(
                d.supportedItems(), d.primaryItems(), d.weight(), d.maxLevel(),
                d.minCost(), new Cost(value, d.maxCost().perLevelAboveFirst()), d.anvilCost(), d.slots()));
    }

    public static UnaryOperator<Enchantment> maxCostPerLevel(int value) {
        return e -> withDefinition(e, d -> new EnchantmentDefinition(
                d.supportedItems(), d.primaryItems(), d.weight(), d.maxLevel(),
                d.minCost(), new Cost(d.maxCost().base(), value), d.anvilCost(), d.slots()));
    }

    public static UnaryOperator<Enchantment> toggleSlot(EquipmentSlotGroup slot) {
        return e -> {
            var slots = new SlotManager(e.definition().slots()).toggle(slot).toGroups();
            return withDefinition(e, d -> new EnchantmentDefinition(
                    d.supportedItems(), d.primaryItems(), d.weight(), d.maxLevel(),
                    d.minCost(), d.maxCost(), d.anvilCost(), slots));
        };
    }

    public static UnaryOperator<Enchantment> supportedItems(HolderSet<Item> items) {
        return e -> withDefinition(e, d -> new EnchantmentDefinition(
                items, d.primaryItems(), d.weight(), d.maxLevel(),
                d.minCost(), d.maxCost(), d.anvilCost(), d.slots()));
    }

    public static UnaryOperator<Enchantment> primaryItems(Optional<HolderSet<Item>> items) {
        return e -> withDefinition(e, d -> new EnchantmentDefinition(
                d.supportedItems(), items, d.weight(), d.maxLevel(),
                d.minCost(), d.maxCost(), d.anvilCost(), d.slots()));
    }

    public static UnaryOperator<Enchantment> exclusiveSet(HolderSet<Enchantment> set) {
        return e -> new Enchantment(e.description(), e.definition(), set, e.effects());
    }

    private EnchantmentMutations() {}
}

package fr.hardel.asset_editor.client.javafx.lib.action;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantment.EnchantmentDefinition;
import net.minecraft.world.item.enchantment.Enchantment.Cost;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

public final class EnchantmentActions {

    public static EditorAction<Enchantment> of(Identifier id, UnaryOperator<Enchantment> transform) {
        return new EditorAction<>("enchantment", id, Enchantment.class, transform);
    }

    public static EditorAction<Enchantment> setMaxLevel(Identifier id, int value) {
        return ofDefinition(id, d -> new EnchantmentDefinition(
                d.supportedItems(), d.primaryItems(), d.weight(), value, d.minCost(), d.maxCost(), d.anvilCost(),
                d.slots()));
    }

    public static EditorAction<Enchantment> setWeight(Identifier id, int value) {
        return ofDefinition(id, d -> new EnchantmentDefinition(
                d.supportedItems(), d.primaryItems(), value, d.maxLevel(), d.minCost(), d.maxCost(), d.anvilCost(),
                d.slots()));
    }

    public static EditorAction<Enchantment> setAnvilCost(Identifier id, int value) {
        return ofDefinition(id, d -> new EnchantmentDefinition(
                d.supportedItems(), d.primaryItems(), d.weight(), d.maxLevel(), d.minCost(), d.maxCost(), value,
                d.slots()));
    }

    public static EditorAction<Enchantment> setMinCostBase(Identifier id, int value) {
        return ofDefinition(id, d -> new EnchantmentDefinition(
                d.supportedItems(), d.primaryItems(), d.weight(), d.maxLevel(),
                new Cost(value, d.minCost().perLevelAboveFirst()), d.maxCost(), d.anvilCost(), d.slots()));
    }

    public static EditorAction<Enchantment> setMinCostPerLevel(Identifier id, int value) {
        return ofDefinition(id, d -> new EnchantmentDefinition(
                d.supportedItems(), d.primaryItems(), d.weight(), d.maxLevel(),
                new Cost(d.minCost().base(), value), d.maxCost(), d.anvilCost(), d.slots()));
    }

    public static EditorAction<Enchantment> setMaxCostBase(Identifier id, int value) {
        return ofDefinition(id, d -> new EnchantmentDefinition(
                d.supportedItems(), d.primaryItems(), d.weight(), d.maxLevel(),
                d.minCost(), new Cost(value, d.maxCost().perLevelAboveFirst()), d.anvilCost(), d.slots()));
    }

    public static EditorAction<Enchantment> setMaxCostPerLevel(Identifier id, int value) {
        return ofDefinition(id, d -> new EnchantmentDefinition(
                d.supportedItems(), d.primaryItems(), d.weight(), d.maxLevel(),
                d.minCost(), new Cost(d.maxCost().base(), value), d.anvilCost(), d.slots()));
    }

    public static EditorAction<Enchantment> toggleSlot(Identifier id, EquipmentSlotGroup slot) {
        return ofDefinition(id, d -> {
            var slots = new ArrayList<>(d.slots());
            if (slots.contains(slot))
                slots.remove(slot);
            else
                slots.add(slot);
            return new EnchantmentDefinition(
                    d.supportedItems(), d.primaryItems(), d.weight(), d.maxLevel(),
                    d.minCost(), d.maxCost(), d.anvilCost(), List.copyOf(slots));
        });
    }

    private static EditorAction<Enchantment> ofDefinition(Identifier id,
            UnaryOperator<EnchantmentDefinition> transform) {
        return of(id,
                e -> new Enchantment(e.description(), transform.apply(e.definition()), e.exclusiveSet(), e.effects()));
    }

    private EnchantmentActions() {
    }
}

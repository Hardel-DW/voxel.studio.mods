package fr.hardel.asset_editor.client.javafx.lib.action;

import fr.hardel.asset_editor.client.javafx.lib.SlotManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantment.Cost;
import net.minecraft.world.item.enchantment.Enchantment.EnchantmentDefinition;

import java.util.ArrayList;
import java.util.List;
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

    public static UnaryOperator<Enchantment> toggleExclusive(Identifier enchantmentId) {
        return e -> {
            Holder<Enchantment> holder = resolveEnchantmentHolder(enchantmentId);
            if (holder == null) return e;

            if (e.exclusiveSet().unwrapKey().isPresent()) {
                // Aligns with web behavior: switching from tag mode to single-id mode.
                return new Enchantment(e.description(), e.definition(), HolderSet.direct(List.of(holder)), e.effects());
            }

            List<Holder<Enchantment>> current = new ArrayList<>(e.exclusiveSet().stream().toList());
            boolean removed = current.removeIf(h -> h.unwrapKey()
                    .map(k -> k.identifier().equals(enchantmentId))
                    .orElse(false));
            if (!removed) current.add(holder);

            HolderSet<Enchantment> newSet = current.isEmpty() ? HolderSet.empty() : HolderSet.direct(current);
            return new Enchantment(e.description(), e.definition(), newSet, e.effects());
        };
    }

    public static HolderSet<Item> resolveItemTag(String path) {
        var conn = Minecraft.getInstance().getConnection();
        if (conn == null) return null;
        return conn.registryAccess().lookup(Registries.ITEM)
                .flatMap(lookup -> lookup.get(TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("minecraft", path))))
                .orElse(null);
    }

    public static HolderSet<Enchantment> resolveEnchantmentTag(String tagPath) {
        Identifier tagId = Identifier.tryParse(tagPath);
        if (tagId == null) return null;
        var conn = Minecraft.getInstance().getConnection();
        if (conn == null) return null;
        return conn.registryAccess().lookup(Registries.ENCHANTMENT)
                .flatMap(lookup -> lookup.get(TagKey.create(Registries.ENCHANTMENT, tagId)))
                .orElse(null);
    }

    private static Holder<Enchantment> resolveEnchantmentHolder(Identifier id) {
        var conn = Minecraft.getInstance().getConnection();
        if (conn == null) return null;
        return conn.registryAccess().lookup(Registries.ENCHANTMENT)
                .flatMap(lookup -> lookup.get(net.minecraft.resources.ResourceKey.create(Registries.ENCHANTMENT, id)))
                .orElse(null);
    }

    private EnchantmentMutations() {}
}

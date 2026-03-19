package fr.hardel.asset_editor.network.workspace.impl;

import fr.hardel.asset_editor.network.workspace.EditorAction;
import fr.hardel.asset_editor.network.workspace.RegistryInterpreter;
import fr.hardel.asset_editor.store.CustomFields;
import fr.hardel.asset_editor.store.ElementEntry;
import fr.hardel.asset_editor.store.EnchantmentFlushAdapter;
import fr.hardel.asset_editor.store.SlotManager;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantment.Cost;
import net.minecraft.world.item.enchantment.Enchantment.EnchantmentDefinition;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class EnchantmentInterpreter implements RegistryInterpreter<Enchantment> {

    private static final String MODE_KEY = EnchantmentFlushAdapter.MODE_KEY;
    private static final String DISABLED_EFFECTS_KEY = EnchantmentFlushAdapter.DISABLED_EFFECTS_KEY;
    private static final String MODE_NORMAL = EnchantmentFlushAdapter.MODE_NORMAL;
    private static final String MODE_DISABLE = EnchantmentFlushAdapter.MODE_DISABLE;

    @Override
    public ElementEntry<Enchantment> apply(ElementEntry<Enchantment> entry, EditorAction action,
        HolderLookup.Provider registries) {
        return switch (action) {
            case EditorAction.SetIntField a -> applyIntField(entry, a.field(), a.value());
            case EditorAction.SetMode a -> applyCustom(entry, custom -> custom.with(MODE_KEY, normalizeMode(a.mode())));
            case EditorAction.ToggleDisabled() -> applyCustom(entry, custom -> {
                String current = custom.getString(MODE_KEY, MODE_NORMAL);
                return custom.with(MODE_KEY, MODE_DISABLE.equals(current) ? MODE_NORMAL : MODE_DISABLE);
            });
            case EditorAction.ToggleDisabledEffect a -> applyCustom(entry, custom -> {
                Set<String> next = new LinkedHashSet<>(custom.getStringSet(DISABLED_EFFECTS_KEY));
                if (!next.remove(a.effectId()))
                    next.add(a.effectId());
                return custom.with(DISABLED_EFFECTS_KEY, next);
            });
            case EditorAction.ToggleSlot a -> {
                EquipmentSlotGroup slot = EquipmentSlotGroup.valueOf(a.slot().toUpperCase());
                List<EquipmentSlotGroup> slots = new SlotManager(entry.data().definition().slots()).toggle(slot).toGroups();
                yield entry.withData(withDefinition(entry.data(), d -> new EnchantmentDefinition(
                    d.supportedItems(), d.primaryItems(), d.weight(), d.maxLevel(),
                    d.minCost(), d.maxCost(), d.anvilCost(), slots)));
            }
            case EditorAction.ToggleTag a -> entry.toggleTag(a.tagId());
            case EditorAction.ToggleExclusive a -> applyToggleExclusive(entry, a.enchantmentId(), registries);
            case EditorAction.SetSupportedItems a -> applySetSupportedItems(entry, a.tagId(), registries);
            case EditorAction.SetPrimaryItems a -> applySetPrimaryItems(entry, a.tagId(), registries);
            case EditorAction.SetExclusiveSet a -> applySetExclusiveSet(entry, a.tagId(), registries);
        };
    }

    private ElementEntry<Enchantment> applyToggleExclusive(ElementEntry<Enchantment> entry,
        Identifier enchantmentId,
        HolderLookup.Provider registries) {
        var lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);
        var holder = lookup.get(ResourceKey.create(Registries.ENCHANTMENT, enchantmentId)).orElse(null);
        if (holder == null)
            return entry;

        Enchantment e = entry.data();
        if (e.exclusiveSet().unwrapKey().isPresent()) {
            return entry.withData(new Enchantment(e.description(), e.definition(),
                HolderSet.direct(List.of(holder)), e.effects()));
        }

        List<Holder<Enchantment>> current = new ArrayList<>(e.exclusiveSet().stream().toList());
        boolean removed = current.removeIf(h -> h.unwrapKey()
            .map(k -> k.identifier().equals(enchantmentId)).orElse(false));
        if (!removed)
            current.add(holder);

        HolderSet<Enchantment> newSet = current.isEmpty() ? HolderSet.empty() : HolderSet.direct(current);
        return entry.withData(new Enchantment(e.description(), e.definition(), newSet, e.effects()));
    }

    private ElementEntry<Enchantment> applySetSupportedItems(ElementEntry<Enchantment> entry,
        String tagId,
        HolderLookup.Provider registries) {
        HolderSet<Item> items = resolveItemTag(tagId, registries);
        if (items == null)
            return entry;
        return entry.withData(withDefinition(entry.data(), d -> new EnchantmentDefinition(
            items, d.primaryItems(), d.weight(), d.maxLevel(),
            d.minCost(), d.maxCost(), d.anvilCost(), d.slots())));
    }

    private ElementEntry<Enchantment> applySetPrimaryItems(ElementEntry<Enchantment> entry,
        String tagId,
        HolderLookup.Provider registries) {
        Optional<HolderSet<Item>> items = tagId.isEmpty()
            ? Optional.empty()
            : Optional.ofNullable(resolveItemTag(tagId, registries));
        if (!tagId.isEmpty() && items.isEmpty())
            return entry;
        return entry.withData(withDefinition(entry.data(), d -> new EnchantmentDefinition(
            d.supportedItems(), items, d.weight(), d.maxLevel(),
            d.minCost(), d.maxCost(), d.anvilCost(), d.slots())));
    }

    private ElementEntry<Enchantment> applySetExclusiveSet(ElementEntry<Enchantment> entry,
        String tagId,
        HolderLookup.Provider registries) {
        Enchantment e = entry.data();
        if (tagId.isEmpty()) {
            return entry.withData(new Enchantment(e.description(), e.definition(), HolderSet.empty(), e.effects()));
        }

        Identifier id = Identifier.tryParse(tagId);
        if (id == null)
            return entry;
        var tag = TagKey.create(Registries.ENCHANTMENT, id);
        HolderSet<Enchantment> resolved = registries.lookupOrThrow(Registries.ENCHANTMENT)
            .get(tag).<HolderSet<Enchantment>> map(named -> named)
            .orElse(HolderSet.empty());
        return entry.withData(new Enchantment(e.description(), e.definition(), resolved, e.effects()));
    }

    private static HolderSet<Item> resolveItemTag(String tagId, HolderLookup.Provider registries) {
        Identifier id = Identifier.tryParse(tagId);
        if (id == null)
            return null;
        var tag = TagKey.create(Registries.ITEM, id);
        return registries.lookupOrThrow(Registries.ITEM)
            .get(tag).<HolderSet<Item>> map(named -> named)
            .orElse(null);
    }

    private ElementEntry<Enchantment> applyIntField(ElementEntry<Enchantment> entry, String field, int value) {
        return entry.withData(withDefinition(entry.data(), d -> switch (field) {
            case "max_level" -> new EnchantmentDefinition(
                d.supportedItems(), d.primaryItems(), d.weight(), value,
                d.minCost(), d.maxCost(), d.anvilCost(), d.slots());
            case "weight" -> new EnchantmentDefinition(
                d.supportedItems(), d.primaryItems(), value, d.maxLevel(),
                d.minCost(), d.maxCost(), d.anvilCost(), d.slots());
            case "anvil_cost" -> new EnchantmentDefinition(
                d.supportedItems(), d.primaryItems(), d.weight(), d.maxLevel(),
                d.minCost(), d.maxCost(), value, d.slots());
            case "min_cost_base" -> new EnchantmentDefinition(
                d.supportedItems(), d.primaryItems(), d.weight(), d.maxLevel(),
                new Cost(value, d.minCost().perLevelAboveFirst()), d.maxCost(), d.anvilCost(), d.slots());
            case "min_cost_per_level" -> new EnchantmentDefinition(
                d.supportedItems(), d.primaryItems(), d.weight(), d.maxLevel(),
                new Cost(d.minCost().base(), value), d.maxCost(), d.anvilCost(), d.slots());
            case "max_cost_base" -> new EnchantmentDefinition(
                d.supportedItems(), d.primaryItems(), d.weight(), d.maxLevel(),
                d.minCost(), new Cost(value, d.maxCost().perLevelAboveFirst()), d.anvilCost(), d.slots());
            case "max_cost_per_level" -> new EnchantmentDefinition(
                d.supportedItems(), d.primaryItems(), d.weight(), d.maxLevel(),
                d.minCost(), new Cost(d.maxCost().base(), value), d.anvilCost(), d.slots());
            default -> d;
        }));
    }

    private static Enchantment withDefinition(Enchantment e, java.util.function.UnaryOperator<EnchantmentDefinition> transform) {
        return new Enchantment(e.description(), transform.apply(e.definition()), e.exclusiveSet(), e.effects());
    }

    private static ElementEntry<Enchantment> applyCustom(ElementEntry<Enchantment> entry,
        java.util.function.UnaryOperator<CustomFields> transform) {
        return entry.withCustom(transform.apply(entry.custom()));
    }

    private static String normalizeMode(String value) {
        return switch (value) {
            case "disable", "only_creative" -> value;
            default -> MODE_NORMAL;
        };
    }
}

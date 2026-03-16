package fr.hardel.asset_editor.network;

import fr.hardel.asset_editor.store.CustomFields;
import fr.hardel.asset_editor.store.ElementEntry;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantment.Cost;
import net.minecraft.world.item.enchantment.Enchantment.EnchantmentDefinition;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class EnchantmentInterpreter implements RegistryInterpreter<Enchantment> {

    private static final String MODE_KEY = "mode";
    private static final String DISABLED_EFFECTS_KEY = "disabledEffects";
    private static final String MODE_NORMAL = "normal";
    private static final String MODE_SOFT_DELETE = "soft_delete";

    @Override
    public ElementEntry<Enchantment> apply(ElementEntry<Enchantment> entry, EditorAction action) {
        return switch (action) {
            case EditorAction.SetIntField a -> applyIntField(entry, a.field(), a.value());
            case EditorAction.SetMode a -> applyCustom(entry, custom -> custom.with(MODE_KEY, normalizeMode(a.mode())));
            case EditorAction.ToggleDisabled() -> applyCustom(entry, custom -> {
                String current = custom.getString(MODE_KEY, MODE_NORMAL);
                return custom.with(MODE_KEY, MODE_SOFT_DELETE.equals(current) ? MODE_NORMAL : MODE_SOFT_DELETE);
            });
            case EditorAction.ToggleDisabledEffect a -> applyCustom(entry, custom -> {
                Set<String> next = new LinkedHashSet<>(custom.getStringSet(DISABLED_EFFECTS_KEY));
                if (!next.remove(a.effectId())) next.add(a.effectId());
                return custom.with(DISABLED_EFFECTS_KEY, next);
            });
            case EditorAction.ToggleSlot a -> {
                EquipmentSlotGroup slot = EquipmentSlotGroup.valueOf(a.slot().toUpperCase());
                List<EquipmentSlotGroup> slots = new ArrayList<>(entry.data().definition().slots());
                if (!slots.remove(slot)) slots.add(slot);
                yield entry.withData(withDefinition(entry.data(), d -> new EnchantmentDefinition(
                        d.supportedItems(), d.primaryItems(), d.weight(), d.maxLevel(),
                        d.minCost(), d.maxCost(), d.anvilCost(), List.copyOf(slots))));
            }
            case EditorAction.ToggleTag a -> entry.toggleTag(a.tagId());
            case EditorAction.ToggleExclusive a -> entry;
        };
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
            case "soft_delete", "only_creative" -> value;
            default -> MODE_NORMAL;
        };
    }
}

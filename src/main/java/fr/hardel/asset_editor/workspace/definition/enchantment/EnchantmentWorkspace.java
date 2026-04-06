package fr.hardel.asset_editor.workspace.definition.enchantment;

import fr.hardel.asset_editor.workspace.CustomFields;
import fr.hardel.asset_editor.workspace.ElementEntry;
import fr.hardel.asset_editor.workspace.definition.SlotManager;
import fr.hardel.asset_editor.workspace.flush.EnchantmentFlushAdapter;
import fr.hardel.asset_editor.tag.TagSeed;
import fr.hardel.asset_editor.workspace.action.enchantment.EnchantmentEditorActions;
import fr.hardel.asset_editor.workspace.definition.WorkspaceDefinition;
import fr.hardel.asset_editor.workspace.RegistryMutationContext;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
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
import java.util.function.UnaryOperator;

public final class EnchantmentWorkspace {

    private static final String MODE_KEY = EnchantmentFlushAdapter.MODE_KEY;
    private static final String DISABLED_EFFECTS_KEY = EnchantmentFlushAdapter.DISABLED_EFFECTS_KEY;
    private static final String MODE_NORMAL = EnchantmentFlushAdapter.MODE_NORMAL;
    private static final String MODE_DISABLE = EnchantmentFlushAdapter.MODE_DISABLE;

    public static void register() {
        WorkspaceDefinition.register(
            WorkspaceDefinition.builder(Registries.ENCHANTMENT, Enchantment.DIRECT_CODEC)
                .flushAdapter(EnchantmentFlushAdapter.INSTANCE)
                .customInitializer(entry -> EnchantmentFlushAdapter.initializeCustom(entry.data(), entry.tags()))
                .action(EnchantmentEditorActions.SET_INT_FIELD,
                    (entry, action, ctx) -> applyIntField(entry, action.field(), action.value()))
                .action(EnchantmentEditorActions.SET_MODE,
                    (entry, action, ctx) -> applyCustom(entry, custom -> custom.with(MODE_KEY, normalizeMode(action.mode()))))
                .action(EnchantmentEditorActions.TOGGLE_DISABLED,
                    (entry, action, ctx) -> applyCustom(entry, custom -> {
                        String current = custom.getString(MODE_KEY, MODE_NORMAL);
                        return custom.with(MODE_KEY, MODE_DISABLE.equals(current) ? MODE_NORMAL : MODE_DISABLE);
                    }))
                .action(EnchantmentEditorActions.TOGGLE_DISABLED_EFFECT,
                    (entry, action, ctx) -> applyCustom(entry, custom -> {
                        Set<String> next = new LinkedHashSet<>(custom.getStringSet(DISABLED_EFFECTS_KEY));
                        if (!next.remove(action.effectId()))
                            next.add(action.effectId());
                        return custom.with(DISABLED_EFFECTS_KEY, next);
                    }))
                .action(EnchantmentEditorActions.TOGGLE_SLOT,
                    (entry, action, ctx) -> {
                        EquipmentSlotGroup slot = EquipmentSlotGroup.valueOf(action.slot().toUpperCase());
                        List<EquipmentSlotGroup> slots = new SlotManager(entry.data().definition().slots()).toggle(slot).toGroups();
                        return entry.withData(withDefinition(entry.data(), d -> new EnchantmentDefinition(
                            d.supportedItems(), d.primaryItems(), d.weight(), d.maxLevel(),
                            d.minCost(), d.maxCost(), d.anvilCost(), slots)));
                    })
                .action(EnchantmentEditorActions.TOGGLE_TAG,
                    (entry, action, ctx) -> entry.toggleTag(action.tagId()))
                .action(EnchantmentEditorActions.TOGGLE_EXCLUSIVE,
                    (entry, action, ctx) -> applyToggleExclusive(entry, action.enchantmentId(), ctx))
                .actionWithHook(EnchantmentEditorActions.SET_SUPPORTED_ITEMS,
                    (entry, action, ctx) -> applySetSupportedItems(entry, action.tagId(), action.seed(), ctx),
                    (action, ctx) -> ensureItemTag(action.tagId(), action.seed(), ctx))
                .actionWithHook(EnchantmentEditorActions.SET_PRIMARY_ITEMS,
                    (entry, action, ctx) -> applySetPrimaryItems(entry, action.tagId(), action.seed(), ctx),
                    (action, ctx) -> ensureItemTag(action.tagId(), action.seed(), ctx))
                .action(EnchantmentEditorActions.SET_EXCLUSIVE_SET,
                    (entry, action, ctx) -> applySetExclusiveSet(entry, action.tagId(), ctx))
                .build());
    }

    private static void ensureItemTag(String rawTagId, TagSeed seed, RegistryMutationContext context) {
        if (seed == null || rawTagId == null || rawTagId.isBlank())
            return;

        Identifier tagId = Identifier.tryParse(rawTagId);
        if (tagId == null)
            throw new IllegalArgumentException("Invalid item tag id: " + rawTagId);

        context.ensureTagResource(Registries.ITEM, tagId, seed);
    }

    private static ElementEntry<Enchantment> applyToggleExclusive(ElementEntry<Enchantment> entry,
        Identifier enchantmentId, RegistryMutationContext context) {
        var lookup = context.registries().lookupOrThrow(Registries.ENCHANTMENT);
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

    private static ElementEntry<Enchantment> applySetSupportedItems(ElementEntry<Enchantment> entry,
        String tagId, TagSeed seed, RegistryMutationContext context) {
        HolderSet<Item> items = resolveItemTag(tagId, seed, context);
        if (items == null)
            return entry;
        return entry.withData(withDefinition(entry.data(), d -> new EnchantmentDefinition(
            items, d.primaryItems(), d.weight(), d.maxLevel(),
            d.minCost(), d.maxCost(), d.anvilCost(), d.slots())));
    }

    private static ElementEntry<Enchantment> applySetPrimaryItems(ElementEntry<Enchantment> entry,
        String tagId, TagSeed seed, RegistryMutationContext context) {
        Optional<HolderSet<Item>> items = tagId == null || tagId.isEmpty()
            ? Optional.empty()
            : Optional.ofNullable(resolveItemTag(tagId, seed, context));
        if (tagId != null && !tagId.isEmpty() && items.isEmpty())
            return entry;
        return entry.withData(withDefinition(entry.data(), d -> new EnchantmentDefinition(
            d.supportedItems(), items, d.weight(), d.maxLevel(),
            d.minCost(), d.maxCost(), d.anvilCost(), d.slots())));
    }

    private static ElementEntry<Enchantment> applySetExclusiveSet(ElementEntry<Enchantment> entry,
        String tagId, RegistryMutationContext context) {
        Enchantment e = entry.data();
        if (tagId.isEmpty()) {
            return entry.withData(new Enchantment(e.description(), e.definition(), HolderSet.empty(), e.effects()));
        }

        Identifier id = Identifier.tryParse(tagId);
        if (id == null)
            return entry;
        HolderSet<Enchantment> resolved = context.resolveTagReference(Registries.ENCHANTMENT, id);
        return entry.withData(new Enchantment(
            e.description(), e.definition(),
            resolved == null ? HolderSet.empty() : resolved,
            e.effects()));
    }

    private static HolderSet<Item> resolveItemTag(String rawTagId, TagSeed seed, RegistryMutationContext context) {
        Identifier id = Identifier.tryParse(rawTagId);
        if (id == null)
            return null;

        return seed == null
            ? context.resolveTagReference(Registries.ITEM, id)
            : context.resolveTagReferenceOrPlaceholder(Registries.ITEM, id);
    }

    private static ElementEntry<Enchantment> applyIntField(ElementEntry<Enchantment> entry, String field, int value) {
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

    private static Enchantment withDefinition(Enchantment e, UnaryOperator<EnchantmentDefinition> transform) {
        return new Enchantment(e.description(), transform.apply(e.definition()), e.exclusiveSet(), e.effects());
    }

    private static ElementEntry<Enchantment> applyCustom(ElementEntry<Enchantment> entry, UnaryOperator<CustomFields> transform) {
        return entry.withCustom(transform.apply(entry.custom()));
    }

    private static String normalizeMode(String value) {
        return switch (value) {
            case "disable", "only_creative" -> value;
            default -> MODE_NORMAL;
        };
    }

    private EnchantmentWorkspace() {}
}

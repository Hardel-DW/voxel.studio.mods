package fr.hardel.asset_editor.client.javafx.lib.action;

import fr.hardel.asset_editor.client.javafx.lib.SlotManager;
import fr.hardel.asset_editor.client.javafx.lib.store.RegistryElementStore.CustomFields;
import fr.hardel.asset_editor.client.javafx.lib.store.RegistryElementStore.ElementEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
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
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public final class EnchantmentMutations {

    public static final String MODE_KEY = "mode";
    public static final String DISABLED_EFFECTS_KEY = "disabledEffects";

    public static final String MODE_NORMAL = "normal";
    public static final String MODE_SOFT_DELETE = "soft_delete";
    public static final String MODE_ONLY_CREATIVE = "only_creative";

    public static final Identifier CURSE_TAG = enchantmentTag("curse");
    public static final Identifier DOUBLE_TRADE_PRICE_TAG = enchantmentTag("double_trade_price");
    public static final Identifier IN_ENCHANTING_TABLE_TAG = enchantmentTag("in_enchanting_table");
    public static final Identifier NON_TREASURE_TAG = enchantmentTag("non_treasure");
    public static final Identifier ON_MOB_SPAWN_EQUIPMENT_TAG = enchantmentTag("on_mob_spawn_equipment");
    public static final Identifier ON_RANDOM_LOOT_TAG = enchantmentTag("on_random_loot");
    public static final Identifier ON_TRADED_EQUIPMENT_TAG = enchantmentTag("on_traded_equipment");
    public static final Identifier PREVENTS_BEE_SPAWNS_WHEN_MINING_TAG = enchantmentTag("prevents_bee_spawns_when_mining");
    public static final Identifier PREVENTS_DECORATED_POT_SHATTERING_TAG = enchantmentTag("prevents_decorated_pot_shattering");
    public static final Identifier PREVENTS_ICE_MELTING_TAG = enchantmentTag("prevents_ice_melting");
    public static final Identifier PREVENTS_INFESTED_SPAWNS_TAG = enchantmentTag("prevents_infested_spawns");
    public static final Identifier SMELTS_LOOT_TAG = enchantmentTag("smelts_loot");
    public static final Identifier TRADEABLE_TAG = enchantmentTag("tradeable");
    public static final Identifier TREASURE_TAG = enchantmentTag("treasure");

    public static final Set<Identifier> FUNCTIONALITY_TAGS = Set.of(
            CURSE_TAG,
            DOUBLE_TRADE_PRICE_TAG,
            PREVENTS_BEE_SPAWNS_WHEN_MINING_TAG,
            PREVENTS_DECORATED_POT_SHATTERING_TAG,
            PREVENTS_ICE_MELTING_TAG,
            PREVENTS_INFESTED_SPAWNS_TAG,
            SMELTS_LOOT_TAG);

    public static final List<Identifier> OVERVIEW_TAGS = List.of(
            IN_ENCHANTING_TABLE_TAG,
            ON_RANDOM_LOOT_TAG,
            ON_TRADED_EQUIPMENT_TAG,
            TRADEABLE_TAG,
            DOUBLE_TRADE_PRICE_TAG);

    public static Enchantment withDefinition(Enchantment e, UnaryOperator<EnchantmentDefinition> transform) {
        return new Enchantment(e.description(), transform.apply(e.definition()), e.exclusiveSet(), e.effects());
    }

    public static CustomFields initializeCustom(Enchantment enchantment, Set<Identifier> tags) {
        return CustomFields.EMPTY
                .with(MODE_KEY, deriveMode(enchantment, tags))
                .with(DISABLED_EFFECTS_KEY, Set.<String>of());
    }

    public static UnaryOperator<CustomFields> mode(String value) {
        String normalized = normalizeMode(value);
        return custom -> custom.with(MODE_KEY, normalized);
    }

    public static UnaryOperator<CustomFields> toggleDisabled() {
        return custom -> mode(isSoftDeleted(custom) ? MODE_NORMAL : MODE_SOFT_DELETE).apply(custom);
    }

    public static UnaryOperator<CustomFields> toggleDisabledEffect(String effectId) {
        return custom -> {
            Set<String> next = new LinkedHashSet<>(disabledEffects(custom));
            if (!next.remove(effectId)) {
                next.add(effectId);
            }
            return custom.with(DISABLED_EFFECTS_KEY, next);
        };
    }

    public static String mode(ElementEntry<Enchantment> entry) {
        return mode(entry.custom());
    }

    public static String mode(CustomFields custom) {
        return normalizeMode(custom.getString(MODE_KEY, MODE_NORMAL));
    }

    public static boolean isSoftDeleted(ElementEntry<Enchantment> entry) {
        return MODE_SOFT_DELETE.equals(mode(entry));
    }

    public static boolean isSoftDeleted(CustomFields custom) {
        return MODE_SOFT_DELETE.equals(mode(custom));
    }

    public static Set<String> disabledEffects(ElementEntry<Enchantment> entry) {
        return disabledEffects(entry.custom());
    }

    public static Set<String> disabledEffects(CustomFields custom) {
        return custom.getStringSet(DISABLED_EFFECTS_KEY);
    }

    public static boolean isEffectDisabled(ElementEntry<Enchantment> entry, String effectId) {
        return isSoftDeleted(entry) || disabledEffects(entry).contains(effectId);
    }

    public static List<String> availableEffects(Enchantment enchantment) {
        return enchantment.effects().keySet().stream()
                .map(EnchantmentMutations::effectId)
                .filter(id -> id != null && !id.isBlank())
                .sorted()
                .toList();
    }

    public static Identifier previewTexture(Enchantment enchantment) {
        Identifier primary = firstItemIdentifier(enchantment.definition().primaryItems());
        if (primary != null) {
            return textureOf(primary);
        }

        Identifier supported = firstItemIdentifier(Optional.of(enchantment.definition().supportedItems()));
        return supported != null ? textureOf(supported) : null;
    }

    public static ElementEntry<Enchantment> prepareForFlush(ElementEntry<Enchantment> entry) {
        String mode = mode(entry);
        Set<String> disabledEffects = disabledEffects(entry);

        Set<Identifier> tags = switch (mode) {
            case MODE_ONLY_CREATIVE -> entry.tags().stream()
                    .filter(FUNCTIONALITY_TAGS::contains)
                    .collect(Collectors.toUnmodifiableSet());
            case MODE_SOFT_DELETE -> Set.of();
            default -> entry.tags();
        };

        Enchantment enchantment = entry.data();
        DataComponentMap effects = enchantment.effects();
        if (MODE_SOFT_DELETE.equals(mode)) {
            effects = DataComponentMap.EMPTY;
        } else if (!disabledEffects.isEmpty() && !effects.isEmpty()) {
            effects = effects.filter(type -> !disabledEffects.contains(effectId(type)));
        }

        HolderSet<Enchantment> exclusiveSet = MODE_SOFT_DELETE.equals(mode) ? HolderSet.empty() : enchantment.exclusiveSet();
        Enchantment prepared = new Enchantment(enchantment.description(), enchantment.definition(), exclusiveSet, effects);

        return entry.withData(prepared).withTags(tags);
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

    public static UnaryOperator<Enchantment> exclusiveSet(Identifier tagId) {
        return e -> {
            HolderSet<Enchantment> holderSet = resolveEnchantmentTag(tagId.toString());
            return holderSet != null ? exclusiveSet(holderSet).apply(e) : e;
        };
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

    public static List<Identifier> customExclusiveTags(List<ElementEntry<Enchantment>> entries) {
        return entries.stream()
                .flatMap(entry -> collectExclusiveTags(entry).stream())
                .filter(tagId -> !"minecraft".equals(tagId.getNamespace()))
                .distinct()
                .sorted(Comparator.comparing(Identifier::toString))
                .toList();
    }

    private static Holder<Enchantment> resolveEnchantmentHolder(Identifier id) {
        var conn = Minecraft.getInstance().getConnection();
        if (conn == null) return null;
        return conn.registryAccess().lookup(Registries.ENCHANTMENT)
                .flatMap(lookup -> lookup.get(ResourceKey.create(Registries.ENCHANTMENT, id)))
                .orElse(null);
    }

    private static String deriveMode(Enchantment enchantment, Set<Identifier> tags) {
        boolean hasEffects = !enchantment.effects().isEmpty();
        boolean hasExclusiveSet = enchantment.exclusiveSet().unwrapKey().isPresent()
                || enchantment.exclusiveSet().stream().findAny().isPresent();

        if (!tags.isEmpty() && tags.stream().allMatch(FUNCTIONALITY_TAGS::contains)) {
            return MODE_ONLY_CREATIVE;
        }
        if (!hasEffects && tags.isEmpty() && !hasExclusiveSet) {
            return MODE_SOFT_DELETE;
        }
        return MODE_NORMAL;
    }

    private static String normalizeMode(String value) {
        if (MODE_SOFT_DELETE.equals(value) || MODE_ONLY_CREATIVE.equals(value)) {
            return value;
        }
        return MODE_NORMAL;
    }

    private static String effectId(DataComponentType<?> type) {
        Identifier id = BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE.getKey(type);
        return id != null ? id.toString() : "";
    }

    private static List<Identifier> collectExclusiveTags(ElementEntry<Enchantment> entry) {
        Set<Identifier> tags = new LinkedHashSet<>();
        entry.data().exclusiveSet().unwrapKey()
                .map(key -> key.location())
                .ifPresent(tags::add);
        entry.tags().stream()
                .filter(tagId -> tagId.getPath().startsWith("exclusive_set/"))
                .forEach(tags::add);
        return List.copyOf(tags);
    }

    private static Identifier firstItemIdentifier(Optional<HolderSet<Item>> items) {
        return items.flatMap(set -> set.stream()
                        .map(holder -> holder.unwrapKey().map(key -> key.identifier()).orElse(null))
                        .filter(java.util.Objects::nonNull)
                        .findFirst())
                .orElseGet(() -> items.flatMap(HolderSet::unwrapKey)
                        .flatMap(key -> resolveFirstItemFromTag(key.location()))
                        .orElse(null));
    }

    private static Optional<Identifier> resolveFirstItemFromTag(Identifier tagId) {
        var conn = Minecraft.getInstance().getConnection();
        if (conn == null) {
            return Optional.empty();
        }

        return conn.registryAccess().lookup(Registries.ITEM)
                .flatMap(lookup -> lookup.get(TagKey.create(Registries.ITEM, tagId)))
                .flatMap(set -> set.stream()
                        .map(holder -> holder.unwrapKey().map(key -> key.identifier()).orElse(null))
                        .filter(java.util.Objects::nonNull)
                        .findFirst());
    }

    private static Identifier textureOf(Identifier itemId) {
        return Identifier.fromNamespaceAndPath(itemId.getNamespace(), "textures/item/" + itemId.getPath() + ".png");
    }

    private static Identifier enchantmentTag(String path) {
        return Identifier.fromNamespaceAndPath("minecraft", path);
    }

    private EnchantmentMutations() {}
}

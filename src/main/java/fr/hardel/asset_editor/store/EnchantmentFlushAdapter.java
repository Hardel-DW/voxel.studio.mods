package fr.hardel.asset_editor.store;

import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Set;
import java.util.stream.Collectors;

public final class EnchantmentFlushAdapter implements FlushAdapter<Enchantment> {

    public static final EnchantmentFlushAdapter INSTANCE = new EnchantmentFlushAdapter();

    public static final String MODE_KEY = "mode";
    public static final String DISABLED_EFFECTS_KEY = "disabledEffects";
    public static final String MODE_NORMAL = "normal";
    public static final String MODE_SOFT_DELETE = "soft_delete";
    public static final String MODE_ONLY_CREATIVE = "only_creative";

    private static final Set<Identifier> FUNCTIONALITY_TAGS = Set.of(
            Identifier.fromNamespaceAndPath("minecraft", "curse"),
            Identifier.fromNamespaceAndPath("minecraft", "double_trade_price"),
            Identifier.fromNamespaceAndPath("minecraft", "prevents_bee_spawns_when_mining"),
            Identifier.fromNamespaceAndPath("minecraft", "prevents_decorated_pot_shattering"),
            Identifier.fromNamespaceAndPath("minecraft", "prevents_ice_melting"),
            Identifier.fromNamespaceAndPath("minecraft", "prevents_infested_spawns"),
            Identifier.fromNamespaceAndPath("minecraft", "smelts_loot"));

    @Override
    public ElementEntry<Enchantment> prepare(ElementEntry<Enchantment> entry) {
        String mode = entry.custom().getString(MODE_KEY, MODE_NORMAL);
        Set<String> disabledEffects = entry.custom().getStringSet(DISABLED_EFFECTS_KEY);

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

    public static CustomFields initializeCustom(Enchantment enchantment, Set<Identifier> tags) {
        return CustomFields.EMPTY
                .with(MODE_KEY, deriveMode(enchantment, tags))
                .with(DISABLED_EFFECTS_KEY, Set.<String>of());
    }

    private static String deriveMode(Enchantment enchantment, Set<Identifier> tags) {
        boolean hasEffects = !enchantment.effects().isEmpty();
        boolean hasExclusiveSet = enchantment.exclusiveSet().unwrapKey().isPresent()
                || enchantment.exclusiveSet().stream().findAny().isPresent();

        if (!tags.isEmpty() && tags.stream().allMatch(FUNCTIONALITY_TAGS::contains))
            return MODE_ONLY_CREATIVE;
        if (!hasEffects && tags.isEmpty() && !hasExclusiveSet)
            return MODE_SOFT_DELETE;
        return MODE_NORMAL;
    }

    private static String effectId(DataComponentType<?> type) {
        Identifier id = BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE.getKey(type);
        return id != null ? id.toString() : "";
    }

    private EnchantmentFlushAdapter() {}
}

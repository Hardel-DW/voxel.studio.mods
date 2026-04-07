package fr.hardel.asset_editor.workspace.flush.adapter;

import fr.hardel.asset_editor.tag.TagHelper;
import fr.hardel.asset_editor.workspace.flush.CustomFields;
import fr.hardel.asset_editor.workspace.flush.ElementEntry;
import fr.hardel.asset_editor.workspace.flush.FlushAdapter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jspecify.annotations.NonNull;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class EnchantmentFlushAdapter implements FlushAdapter<Enchantment> {

    public static final EnchantmentFlushAdapter INSTANCE = new EnchantmentFlushAdapter();
    public static final String DISABLED_EFFECTS_KEY = "disabledEffects";
    public static final Identifier CURSE_TAG = Identifier.withDefaultNamespace("curse");
    public static final Identifier DOUBLE_TRADE_PRICE_TAG = Identifier.withDefaultNamespace("double_trade_price");
    public static final Identifier PREVENTS_BEE_SPAWNS_WHEN_MINING_TAG = Identifier.withDefaultNamespace("prevents_bee_spawns_when_mining");
    public static final Identifier PREVENTS_DECORATED_POT_SHATTERING_TAG = Identifier.withDefaultNamespace("prevents_decorated_pot_shattering");
    public static final Identifier PREVENTS_ICE_MELTING_TAG = Identifier.withDefaultNamespace("prevents_ice_melting");
    public static final Identifier PREVENTS_INFESTED_SPAWNS_TAG = Identifier.withDefaultNamespace("prevents_infested_spawns");
    public static final Identifier SMELTS_LOOT_TAG = Identifier.withDefaultNamespace("smelts_loot");

    public static final Set<Identifier> FUNCTIONALITY_TAGS = Set.of(
        CURSE_TAG, DOUBLE_TRADE_PRICE_TAG,
        PREVENTS_BEE_SPAWNS_WHEN_MINING_TAG, PREVENTS_DECORATED_POT_SHATTERING_TAG,
        PREVENTS_ICE_MELTING_TAG, PREVENTS_INFESTED_SPAWNS_TAG, SMELTS_LOOT_TAG);

    @Override
    public ElementEntry<Enchantment> prepare(ElementEntry<Enchantment> entry) {
        EnchantmentMode mode = mode(entry);
        Set<String> disabledEffects = disabledEffects(entry);
        Set<Identifier> tags = switch (mode) {
            case ONLY_CREATIVE -> entry.tags().stream().filter(FUNCTIONALITY_TAGS::contains).collect(Collectors.toUnmodifiableSet());
            case DISABLE -> Set.of();
            default -> entry.tags();
        };

        Enchantment prepared = prepareEnchantment(entry, mode, disabledEffects);
        return entry.withData(prepared).withTags(tags);
    }

    private static @NonNull Enchantment prepareEnchantment(ElementEntry<Enchantment> entry, EnchantmentMode mode, Set<String> disabledEffects) {
        Enchantment enchantment = entry.data();
        DataComponentMap effects = enchantment.effects();
        if (mode == EnchantmentMode.DISABLE) {
            effects = DataComponentMap.EMPTY;
        } else if (!disabledEffects.isEmpty() && !effects.isEmpty()) {
            effects = effects.filter(type -> !disabledEffects.contains(effectId(type)));
        }

        HolderSet<Enchantment> exclusiveSet = mode == EnchantmentMode.DISABLE ? HolderSet.empty() : enchantment.exclusiveSet();
        return new Enchantment(enchantment.description(), enchantment.definition(), exclusiveSet, effects);
    }

    public static CustomFields initializeCustom(Enchantment enchantment, Set<Identifier> tags) {
        return CustomFields.EMPTY
            .with(EnchantmentMode.CUSTOM_FIELD_KEY, deriveMode(enchantment, tags).id())
            .with(DISABLED_EFFECTS_KEY, Set.<String> of());
    }

    public static EnchantmentMode mode(CustomFields custom) {
        return EnchantmentMode.fromId(custom.getString(EnchantmentMode.CUSTOM_FIELD_KEY, EnchantmentMode.NORMAL.id()));
    }

    public static EnchantmentMode mode(ElementEntry<Enchantment> entry) {
        return mode(entry.custom());
    }

    public static boolean isSoftDeleted(ElementEntry<Enchantment> entry) {
        return mode(entry) == EnchantmentMode.DISABLE;
    }

    public static Set<String> disabledEffects(CustomFields custom) {
        return custom.getStringSet(DISABLED_EFFECTS_KEY);
    }

    public static Set<String> disabledEffects(ElementEntry<Enchantment> entry) {
        return disabledEffects(entry.custom());
    }

    public static boolean isEffectDisabled(ElementEntry<Enchantment> entry, String effectId) {
        return isSoftDeleted(entry) || disabledEffects(entry).contains(effectId);
    }

    public static List<String> availableEffects(Enchantment enchantment) {
        return enchantment.effects().keySet().stream()
            .map(EnchantmentFlushAdapter::effectId)
            .filter(id -> !id.isBlank())
            .sorted()
            .toList();
    }

    public static Identifier previewItemId(Enchantment enchantment,
        Function<TagKey<Item>, Optional<HolderSet<Item>>> itemTagResolver) {
        Identifier primary = TagHelper.firstIdentifier(enchantment.definition().primaryItems().orElse(null), itemTagResolver);
        return primary != null ? primary : TagHelper.firstIdentifier(enchantment.definition().supportedItems(), itemTagResolver);
    }

    public static List<Identifier> customExclusiveTags(List<ElementEntry<Enchantment>> entries) {
        return entries.stream()
            .flatMap(entry -> collectExclusiveTags(entry).stream())
            .filter(tagId -> !Identifier.DEFAULT_NAMESPACE.equals(tagId.getNamespace()))
            .distinct()
            .sorted(Comparator.comparing(Identifier::toString))
            .toList();
    }

    private static EnchantmentMode deriveMode(Enchantment enchantment, Set<Identifier> tags) {
        boolean hasEffects = !enchantment.effects().isEmpty();
        boolean hasExclusiveSet = enchantment.exclusiveSet().unwrapKey().isPresent()
            || enchantment.exclusiveSet().stream().findAny().isPresent();

        if (!tags.isEmpty() && FUNCTIONALITY_TAGS.containsAll(tags))
            return EnchantmentMode.ONLY_CREATIVE;
        if (!hasEffects && tags.isEmpty() && !hasExclusiveSet)
            return EnchantmentMode.DISABLE;
        return EnchantmentMode.NORMAL;
    }

    private static String effectId(DataComponentType<?> type) {
        Identifier id = BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE.getKey(type);
        return id != null ? id.toString() : "";
    }

    private static List<Identifier> collectExclusiveTags(ElementEntry<Enchantment> entry) {
        Set<Identifier> tags = new LinkedHashSet<>();
        entry.data().exclusiveSet().unwrapKey().map(TagKey::location).ifPresent(tags::add);
        tags.addAll(TagHelper.filterByPathPrefix(entry.tags(), "exclusive_set/"));
        return List.copyOf(tags);
    }

    private EnchantmentFlushAdapter() {}

    public enum EnchantmentMode {
        NORMAL("normal"),
        DISABLE("disable"),
        ONLY_CREATIVE("only_creative");

        public static final String CUSTOM_FIELD_KEY = "mode";

        private final String id;

        EnchantmentMode(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public static EnchantmentMode fromId(String value) {
            if (value == null)
                return NORMAL;
            return switch (value) {
                case "disable" -> DISABLE;
                case "only_creative" -> ONLY_CREATIVE;
                default -> NORMAL;
            };
        }
    }
}

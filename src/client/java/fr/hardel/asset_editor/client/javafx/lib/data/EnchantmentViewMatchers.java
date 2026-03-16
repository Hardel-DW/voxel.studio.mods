package fr.hardel.asset_editor.client.javafx.lib.data;

import fr.hardel.asset_editor.client.javafx.lib.data.SlotConfigs.SlotConfig;
import fr.hardel.asset_editor.store.ElementEntry;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class EnchantmentViewMatchers {

    @FunctionalInterface
    public interface ViewMatcher {
        boolean matches(ElementEntry<Enchantment> entry, String category);
    }

    private static final Map<StudioSidebarView, ViewMatcher> MATCHERS = Map.of(
            StudioSidebarView.SLOTS, EnchantmentViewMatchers::matchesSlot,
            StudioSidebarView.ITEMS, EnchantmentViewMatchers::matchesItem,
            StudioSidebarView.EXCLUSIVE, EnchantmentViewMatchers::matchesExclusive
    );

    public static boolean matches(ElementEntry<Enchantment> entry, String filterPath, StudioSidebarView sidebarView) {
        if (filterPath.isEmpty()) return true;

        String[] parts = filterPath.split("/", 2);
        String category = parts[0];
        String leaf = parts.length == 2 ? parts[1] : "";
        if (!leaf.isEmpty() && !entry.id().getPath().equals(leaf)) return false;

        ViewMatcher matcher = MATCHERS.get(sidebarView);
        return matcher != null && matcher.matches(entry, category);
    }

    public static boolean matchesSlot(ElementEntry<Enchantment> entry, String category) {
        SlotConfig config = SlotConfigs.BY_ID.get(category);
        if (config == null) return false;
        return entry.data().definition().slots().stream()
                .anyMatch(g -> config.slots().contains(g.getSerializedName()));
    }

    public static boolean matchesItem(ElementEntry<Enchantment> entry, String category) {
        boolean supported = entry.data().definition().supportedItems().unwrapKey()
                .map(tag -> tag.location().getPath().equals(category))
                .orElse(false);
        if (supported) return true;

        boolean primary = entry.data().definition().primaryItems()
                .flatMap(hs -> hs.unwrapKey())
                .map(tag -> tag.location().getPath().equals(category))
                .orElse(false);
        if (primary) return true;

        return entry.tags().stream().anyMatch(t -> t.getPath().equals(category));
    }

    private static boolean matchesExclusive(ElementEntry<Enchantment> entry, String category) {
        var tagKey = entry.data().exclusiveSet().unwrapKey();
        if (tagKey.isPresent()) {
            String full = tagKey.get().location().toString().toLowerCase(Locale.ROOT);
            String path = tagKey.get().location().getPath().toLowerCase(Locale.ROOT);
            return full.equals(category) || path.equals(category);
        }
        return entry.data().exclusiveSet().stream()
                .map(holder -> holder.unwrapKey().map(k -> k.identifier()).orElse(null))
                .filter(Objects::nonNull)
                .anyMatch(id -> id.toString().toLowerCase(Locale.ROOT).equals(category)
                        || id.getPath().toLowerCase(Locale.ROOT).equals(category));
    }

    private EnchantmentViewMatchers() {}
}

package fr.hardel.asset_editor.client.javafx.data.mock;

import fr.hardel.asset_editor.client.javafx.data.StudioSidebarView;
import fr.hardel.asset_editor.client.javafx.components.ui.tree.TreeTextUtil;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class StudioMockRepository {

    private static final Map<String, List<String>> SLOT_MATCHERS = Map.of(
            "mainhand", List.of("mainhand", "any", "hand", "all"),
            "offhand", List.of("offhand", "any", "hand", "all"),
            "body", List.of("body", "any", "all"),
            "saddle", List.of("saddle", "any", "all"),
            "head", List.of("head", "any", "armor", "all"),
            "chest", List.of("chest", "any", "armor", "all"),
            "legs", List.of("legs", "any", "armor", "all"),
            "feet", List.of("feet", "any", "armor", "all")
    );

    private final List<StudioMockEnchantment> enchantments = List.of(
            new StudioMockEnchantment("minecraft", "sharpness", 5, 10, 1, List.of("mainhand"), List.of("sword"), "damage"),
            new StudioMockEnchantment("minecraft", "smite", 5, 5, 1, List.of("mainhand"), List.of("sword", "axe"), "damage"),
            new StudioMockEnchantment("minecraft", "bane_of_arthropods", 5, 5, 1, List.of("mainhand"), List.of("sword", "axe"), "damage"),
            new StudioMockEnchantment("minecraft", "protection", 4, 10, 1, List.of("armor"), List.of("helmet", "chestplate", "leggings", "boots"), "protection"),
            new StudioMockEnchantment("minecraft", "fire_protection", 4, 5, 2, List.of("armor"), List.of("helmet", "chestplate", "leggings", "boots"), "protection"),
            new StudioMockEnchantment("minecraft", "feather_falling", 4, 5, 2, List.of("feet"), List.of("boots"), "mobility"),
            new StudioMockEnchantment("minecraft", "efficiency", 5, 10, 1, List.of("mainhand"), List.of("pickaxe", "axe", "shovel", "hoe"), "utility"),
            new StudioMockEnchantment("minecraft", "fortune", 3, 2, 4, List.of("mainhand"), List.of("pickaxe", "axe", "shovel", "hoe"), "utility"),
            new StudioMockEnchantment("minecraft", "silk_touch", 1, 1, 8, List.of("mainhand"), List.of("pickaxe", "axe", "shovel", "hoe"), "utility"),
            new StudioMockEnchantment("minecraft", "mending", 1, 2, 4, List.of("all"), List.of("sword", "pickaxe", "armor", "bow", "crossbow", "trident"), "rare"),
            new StudioMockEnchantment("minecraft", "unbreaking", 3, 5, 2, List.of("all"), List.of("sword", "pickaxe", "armor", "bow", "crossbow", "trident"), "durability"),
            new StudioMockEnchantment("minecraft", "looting", 3, 2, 4, List.of("mainhand"), List.of("sword"), "damage")
    );

    public List<StudioMockEnchantment> enchantments() {
        return enchantments;
    }

    public List<StudioMockEnchantment> filter(String search, String filterPath, StudioSidebarView sidebarView) {
        String normalizedSearch = search == null ? "" : search.trim().toLowerCase();
        String normalizedPath = filterPath == null ? "" : filterPath.trim().toLowerCase();
        if (normalizedSearch.isEmpty() && normalizedPath.isEmpty())
            return enchantments;
        return enchantments.stream()
                .filter(enchantment -> matchesSearch(enchantment, normalizedSearch))
                .filter(enchantment -> matchesFilter(enchantment, normalizedPath, sidebarView))
                .toList();
    }

    public Collection<String> groups(StudioSidebarView mode) {
        LinkedHashSet<String> groups = new LinkedHashSet<>();
        for (StudioMockEnchantment enchantment : enchantments) {
            if (mode == StudioSidebarView.EXCLUSIVE) {
                groups.add(TreeTextUtil.toDisplay(enchantment.exclusiveGroup()));
                continue;
            }
            if (mode == StudioSidebarView.SLOTS) {
                for (Map.Entry<String, List<String>> entry : SLOT_MATCHERS.entrySet()) {
                    if (hasAny(enchantment.slots(), entry.getValue())) {
                        groups.add(entry.getKey());
                    }
                }
                continue;
            }
            groups.addAll(enchantment.items());
        }
        return groups;
    }

    private boolean matchesSearch(StudioMockEnchantment enchantment, String search) {
        if (search.isEmpty())
            return true;
        String id = enchantment.resource().toLowerCase();
        return id.contains(search);
    }

    private boolean matchesFilter(StudioMockEnchantment enchantment, String filterPath, StudioSidebarView sidebarView) {
        if (filterPath.isEmpty())
            return true;
        String[] parts = filterPath.split("/", 2);
        String category = parts[0];
        String leaf = parts.length == 2 ? parts[1] : "";
        if (!leaf.isEmpty() && !enchantment.resource().equals(leaf))
            return false;
        if (sidebarView == StudioSidebarView.EXCLUSIVE)
            return TreeTextUtil.toDisplay(enchantment.exclusiveGroup()).toLowerCase().equals(category);
        if (sidebarView == StudioSidebarView.SLOTS)
            return hasAny(enchantment.slots(), SLOT_MATCHERS.getOrDefault(category, List.of()));
        return enchantment.items().contains(category);
    }

    private static boolean hasAny(List<String> source, List<String> expected) {
        for (String value : source) {
            if (expected.contains(value)) return true;
        }
        return false;
    }
}



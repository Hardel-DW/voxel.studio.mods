package fr.hardel.asset_editor.client.javafx.components.page.enchantment;

import fr.hardel.asset_editor.client.javafx.components.ui.tree.TreeNodeModel;
import fr.hardel.asset_editor.client.javafx.lib.data.EnchantmentTreeData;
import fr.hardel.asset_editor.client.javafx.lib.data.SlotConfigs;
import fr.hardel.asset_editor.client.javafx.lib.data.SlotConfigs.SlotConfig;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioSidebarView;
import fr.hardel.asset_editor.client.javafx.lib.store.RegistryElementStore.ElementEntry;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class EnchantmentTreeBuilder {

    public static TreeNodeModel build(Collection<ElementEntry<?>> entries, StudioSidebarView view, int version) {
        List<ElementEntry<Enchantment>> enchantments = castEntries(entries);

        TreeNodeModel root = new TreeNodeModel();
        root.setCount(enchantments.size());
        if (view == StudioSidebarView.SLOTS) {
            buildBySlots(root, enchantments);
            return root;
        }
        if (view == StudioSidebarView.ITEMS) {
            buildByItems(root, enchantments, version);
            return root;
        }
        buildByExclusive(root, enchantments);
        return root;
    }

    public static Map<String, Identifier> slotFolderIcons() {
        LinkedHashMap<String, Identifier> icons = new LinkedHashMap<>();
        for (SlotConfig config : SlotConfigs.ALL) icons.put(config.id(), config.image());
        return icons;
    }

    public static Map<String, Identifier> itemFolderIcons(int version) {
        LinkedHashMap<String, Identifier> icons = new LinkedHashMap<>();
        for (EnchantmentTreeData.ItemTagConfig tag : EnchantmentTreeData.ITEM_TAGS) {
            if (version < tag.min() || version > tag.max()) continue;
            icons.put(tag.key(), tag.icon());
        }
        return icons;
    }

    private static boolean matchesItemCategory(ElementEntry<Enchantment> entry, String key) {
        String itemTag = "enchantable/" + key;
        boolean supported = entry.data().definition().supportedItems().unwrapKey()
                .map(k -> k.location().getPath().equals(itemTag))
                .orElse(false);
        if (supported) return true;
        boolean primary = entry.data().definition().primaryItems()
                .flatMap(hs -> hs.unwrapKey())
                .map(k -> k.location().getPath().equals(itemTag))
                .orElse(false);
        if (primary) return true;
        return entry.tags().stream().anyMatch(t -> t.getPath().equals(itemTag));
    }

    private static void buildBySlots(TreeNodeModel root, List<ElementEntry<Enchantment>> enchantments) {
        for (SlotConfig config : SlotConfigs.ALL) {
            List<ElementEntry<Enchantment>> matching = enchantments.stream()
                    .filter(e -> e.data().definition().slots().stream()
                            .anyMatch(g -> config.slots().contains(g.getSerializedName())))
                    .toList();
            if (!matching.isEmpty()) {
                TreeNodeModel category = createCategoryNode(matching);
                category.setIcon(config.image());
                category.setLabel(translationOrDefault(config.nameKey(), config.id()));
                root.children().put(config.id(), category);
            }
        }
    }

    private static void buildByItems(TreeNodeModel root, List<ElementEntry<Enchantment>> enchantments, int version) {
        for (EnchantmentTreeData.ItemTagConfig tag : EnchantmentTreeData.ITEM_TAGS) {
            if (version < tag.min() || version > tag.max()) continue;
            List<ElementEntry<Enchantment>> matching = enchantments.stream()
                    .filter(e -> matchesItemCategory(e, tag.key()))
                    .toList();
            if (!matching.isEmpty()) {
                TreeNodeModel category = createCategoryNode(matching);
                category.setIcon(tag.icon());
                category.setLabel(translationOrDefault("enchantment:supported." + tag.key() + ".title", tag.key()));
                root.children().put(tag.key(), category);
            }
        }
    }

    private static void buildByExclusive(TreeNodeModel root, List<ElementEntry<Enchantment>> enchantments) {
        LinkedHashMap<String, List<ElementEntry<Enchantment>>> grouped = new LinkedHashMap<>();
        for (var entry : enchantments) {
            var tagKey = entry.data().exclusiveSet().unwrapKey();
            if (tagKey.isPresent()) {
                grouped.computeIfAbsent(tagKey.get().location().getPath(), k -> new ArrayList<>()).add(entry);
                continue;
            }

            List<Identifier> directIds = entry.data().exclusiveSet().stream()
                    .map(holder -> holder.unwrapKey().map(k -> k.identifier()).orElse(null))
                    .filter(Objects::nonNull)
                    .toList();
            if (directIds.isEmpty()) {
                grouped.computeIfAbsent("none", k -> new ArrayList<>()).add(entry);
                continue;
            }
            for (Identifier id : directIds) {
                grouped.computeIfAbsent(id.toString(), k -> new ArrayList<>()).add(entry);
            }
        }
        for (var e : grouped.entrySet()) {
            TreeNodeModel category = createCategoryNode(e.getValue());
            String key = e.getKey();
            String fallback = key.contains(":") ? key.substring(key.lastIndexOf(':') + 1) : key;
            category.setLabel(translationOrDefault("enchantment:exclusive.set." + key + ".title", fallback));
            root.children().put(e.getKey(), category);
        }
    }

    private static TreeNodeModel createCategoryNode(List<ElementEntry<Enchantment>> enchantments) {
        TreeNodeModel node = new TreeNodeModel();
        node.setCount(enchantments.size());
        for (var entry : enchantments) {
            TreeNodeModel leaf = new TreeNodeModel();
            leaf.setCount(0);
            leaf.setElementId(entry.id().toString());
            leaf.setLabel(entry.data().description().getString());
            node.children().put(entry.id().getPath(), leaf);
        }
        return node;
    }

    @SuppressWarnings("unchecked")
    private static List<ElementEntry<Enchantment>> castEntries(Collection<ElementEntry<?>> entries) {
        return entries.stream().map(e -> (ElementEntry<Enchantment>) e).toList();
    }

    private static String translationOrDefault(String key, String fallback) {
        return I18n.exists(key) ? I18n.get(key) : fallback;
    }

    private EnchantmentTreeBuilder() {}
}

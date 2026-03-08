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

public final class EnchantmentTreeBuilder {

    @SuppressWarnings("unchecked")
    public static TreeNodeModel build(Collection<ElementEntry<?>> entries, StudioSidebarView view, int version) {
        List<ElementEntry<Enchantment>> enchantments = entries.stream()
                .map(e -> (ElementEntry<Enchantment>) e)
                .toList();

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
        for (SlotConfig config : SlotConfigs.ALL) {
            icons.put(config.id(), config.image());
        }
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
                    .filter(e -> e.data().definition().supportedItems().unwrapKey()
                            .map(k -> k.location().getPath().endsWith("/" + tag.key()))
                            .orElse(false))
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
            String set = entry.data().exclusiveSet().unwrapKey()
                    .map(tag -> tag.location().getPath())
                    .orElse("none");
            grouped.computeIfAbsent(set, k -> new ArrayList<>()).add(entry);
        }
        for (var e : grouped.entrySet()) {
            TreeNodeModel category = createCategoryNode(e.getValue());
            category.setLabel(translationOrDefault("enchantment:exclusive.set." + e.getKey() + ".title", e.getKey()));
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

    private static String translationOrDefault(String key, String fallback) {
        return I18n.exists(key) ? I18n.get(key) : fallback;
    }

    private EnchantmentTreeBuilder() {}
}

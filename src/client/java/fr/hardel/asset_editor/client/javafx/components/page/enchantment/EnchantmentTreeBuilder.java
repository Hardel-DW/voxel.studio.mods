package fr.hardel.asset_editor.client.javafx.components.page.enchantment;

import fr.hardel.asset_editor.client.javafx.components.ui.tree.TreeNodeModel;
import fr.hardel.asset_editor.client.javafx.lib.data.EnchantmentTreeData;
import fr.hardel.asset_editor.client.javafx.lib.data.SlotConfigs;
import fr.hardel.asset_editor.client.javafx.lib.data.SlotConfigs.SlotConfig;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioSidebarView;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EnchantmentTreeBuilder {

    public static TreeNodeModel build(List<Holder.Reference<Enchantment>> enchantments, StudioSidebarView view, int version) {
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

    private static void buildBySlots(TreeNodeModel root, List<Holder.Reference<Enchantment>> enchantments) {
        for (SlotConfig config : SlotConfigs.ALL) {
            ArrayList<Holder.Reference<Enchantment>> matching = new ArrayList<>();
            for (var holder : enchantments) {
                if (holder.value().definition().slots().stream()
                        .anyMatch(g -> config.slots().contains(g.getSerializedName()))) {
                    matching.add(holder);
                }
            }
            if (!matching.isEmpty()) {
                TreeNodeModel category = createCategoryNode(matching);
                category.setIcon(config.image());
                category.setLabel(translationOrDefault(config.nameKey(), config.id()));
                root.children().put(config.id(), category);
            }
        }
    }

    private static void buildByItems(TreeNodeModel root, List<Holder.Reference<Enchantment>> enchantments, int version) {
        for (EnchantmentTreeData.ItemTagConfig tag : EnchantmentTreeData.ITEM_TAGS) {
            if (version < tag.min() || version > tag.max()) continue;
            ArrayList<Holder.Reference<Enchantment>> matching = new ArrayList<>();
            for (var holder : enchantments) {
                boolean matches = holder.value().definition().supportedItems().unwrapKey()
                        .map(k -> k.location().getPath().endsWith("/" + tag.key()))
                        .orElse(false);
                if (matches) matching.add(holder);
            }
            if (!matching.isEmpty()) {
                TreeNodeModel category = createCategoryNode(matching);
                category.setIcon(tag.icon());
                category.setLabel(translationOrDefault("enchantment:supported." + tag.key() + ".title", tag.key()));
                root.children().put(tag.key(), category);
            }
        }
    }

    private static void buildByExclusive(TreeNodeModel root, List<Holder.Reference<Enchantment>> enchantments) {
        LinkedHashMap<String, ArrayList<Holder.Reference<Enchantment>>> grouped = new LinkedHashMap<>();
        for (var holder : enchantments) {
            String set = holder.value().exclusiveSet().unwrapKey()
                    .map(tag -> tag.location().getPath())
                    .orElse("none");
            grouped.computeIfAbsent(set, key -> new ArrayList<>()).add(holder);
        }
        for (Map.Entry<String, ArrayList<Holder.Reference<Enchantment>>> entry : grouped.entrySet()) {
            String key = entry.getKey();
            TreeNodeModel category = createCategoryNode(entry.getValue());
            category.setLabel(translationOrDefault("enchantment:exclusive.set." + key + ".title", key));
            root.children().put(key, category);
        }
    }

    private static TreeNodeModel createCategoryNode(List<Holder.Reference<Enchantment>> enchantments) {
        TreeNodeModel node = new TreeNodeModel();
        node.setCount(enchantments.size());
        for (var holder : enchantments) {
            TreeNodeModel leaf = new TreeNodeModel();
            leaf.setCount(0);
            leaf.setElementId(holder.key().identifier().toString());
            leaf.setLabel(holder.value().description().getString());
            node.children().put(holder.key().identifier().getPath(), leaf);
        }
        return node;
    }

    private static String translationOrDefault(String key, String fallback) {
        return I18n.exists(key) ? I18n.get(key) : fallback;
    }

    private EnchantmentTreeBuilder() {}
}

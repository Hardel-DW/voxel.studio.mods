package fr.hardel.asset_editor.client.javafx.components.page.enchantment;

import fr.hardel.asset_editor.client.javafx.components.ui.tree.TreeNodeModel;
import fr.hardel.asset_editor.client.javafx.lib.data.EnchantmentTreeData;
import fr.hardel.asset_editor.client.javafx.lib.data.EnchantmentViewMatchers;
import fr.hardel.asset_editor.client.javafx.lib.data.SlotConfigs;
import fr.hardel.asset_editor.client.javafx.lib.data.SlotConfigs.SlotConfig;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioSidebarView;
import fr.hardel.asset_editor.client.javafx.lib.text.StudioText;
import fr.hardel.asset_editor.client.javafx.lib.store.RegistryElementStore.ElementEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class EnchantmentTreeBuilder {

    public static TreeNodeModel build(Collection<ElementEntry<?>> entries, StudioSidebarView view) {
        List<ElementEntry<Enchantment>> enchantments = castEntries(entries);

        TreeNodeModel root = new TreeNodeModel();
        root.setCount(enchantments.size());
        if (view == StudioSidebarView.SLOTS) {
            buildBySlots(root, enchantments);
            return root;
        }
        if (view == StudioSidebarView.ITEMS) {
            buildByItems(root, enchantments);
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

    public static Map<String, Identifier> itemFolderIcons() {
        LinkedHashMap<String, Identifier> icons = new LinkedHashMap<>();
        for (EnchantmentTreeData.ItemTagConfig tag : EnchantmentTreeData.ITEM_TAGS) {
            icons.put(tag.key(), tag.icon());
        }
        return icons;
    }

    private static void buildBySlots(TreeNodeModel root, List<ElementEntry<Enchantment>> enchantments) {
        for (SlotConfig config : SlotConfigs.ALL) {
            List<ElementEntry<Enchantment>> matching = enchantments.stream()
                    .filter(e -> EnchantmentViewMatchers.matchesSlot(e, config.id()))
                    .toList();
            if (!matching.isEmpty()) {
                TreeNodeModel category = createCategoryNode(matching);
                category.setIcon(config.image());
                category.setLabel(StudioText.resolve(StudioText.Domain.SLOT, config.id()));
                root.children().put(config.id(), category);
            }
        }
    }

    private static void buildByItems(TreeNodeModel root, List<ElementEntry<Enchantment>> enchantments) {
        for (EnchantmentTreeData.ItemTagConfig tag : EnchantmentTreeData.ITEM_TAGS) {
            List<ElementEntry<Enchantment>> matching = enchantments.stream()
                    .filter(e -> EnchantmentViewMatchers.matchesItem(e, tag.key()))
                    .toList();
            if (!matching.isEmpty()) {
                TreeNodeModel category = createCategoryNode(matching);
                category.setIcon(tag.icon());
                category.setLabel(StudioText.resolve(StudioText.Domain.ENCHANTMENT_SUPPORTED, tag.key()));
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
                continue;
            }
            for (Identifier id : directIds) {
                grouped.computeIfAbsent(id.toString(), k -> new ArrayList<>()).add(entry);
            }
        }
        for (var e : grouped.entrySet()) {
            TreeNodeModel category = createCategoryNode(e.getValue());
            Identifier tagId = Identifier.tryParse(e.getKey());
            String setName = tagId == null ? e.getKey()
                    : tagId.getPath().substring(tagId.getPath().lastIndexOf('/') + 1);
            category.setLabel(StudioText.resolve(StudioText.Domain.ENCHANTMENT_EXCLUSIVE, setName));
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

    private EnchantmentTreeBuilder() {}
}

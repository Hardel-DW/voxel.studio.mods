package fr.hardel.asset_editor.client.compose.components.page.enchantment

import fr.hardel.asset_editor.client.compose.components.ui.tree.TreeNodeModel
import fr.hardel.asset_editor.client.compose.lib.StudioText
import fr.hardel.asset_editor.client.compose.lib.SlotConfigs
import fr.hardel.asset_editor.workspace.ElementEntry
import java.util.Locale
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier
import net.minecraft.world.item.enchantment.Enchantment

object EnchantmentTreeBuilder {

    fun build(
        enchantments: List<ElementEntry<Enchantment>>,
        view: StudioSidebarView
    ): TreeNodeModel {
        val root = TreeNodeModel()
        root.count = enchantments.size

        when (view) {
            StudioSidebarView.SLOTS -> buildBySlots(root, enchantments)
            StudioSidebarView.ITEMS -> buildByItems(root, enchantments)
            StudioSidebarView.EXCLUSIVE -> buildByExclusive(root, enchantments)
        }

        return root
    }

    fun slotFolderIcons(): Map<String, Identifier> =
        LinkedHashMap<String, Identifier>().apply {
            SlotConfigs.PHYSICAL.forEach { id -> put(id, SlotConfigs.slotImage(id)) }
        }

    fun itemFolderIcons(): Map<String, Identifier> =
        LinkedHashMap<String, Identifier>().apply {
            EnchantmentTreeData.ITEM_TAGS.forEach { tag -> put(tag.key(), tag.icon()) }
        }

    private fun buildBySlots(root: TreeNodeModel, enchantments: List<ElementEntry<Enchantment>>) {
        SlotConfigs.PHYSICAL.forEach { slotId ->
            val matching = enchantments.filter { entry -> EnchantmentViewMatchers.matchesSlot(entry, slotId) }
            if (matching.isEmpty()) {
                return@forEach
            }

            val category = createCategoryNode(matching)
            category.icon = SlotConfigs.slotImage(slotId)
            category.label = I18n.get("slot:$slotId")
            root.children[slotId] = category
        }
    }

    private fun buildByItems(root: TreeNodeModel, enchantments: List<ElementEntry<Enchantment>>) {
        EnchantmentTreeData.ITEM_TAGS.forEach { tag ->
            val matching = enchantments.filter { entry -> EnchantmentViewMatchers.matchesItem(entry, tag.key()) }
            if (matching.isEmpty()) {
                return@forEach
            }

            val category = createCategoryNode(matching)
            category.icon = tag.icon()
            category.label = StudioText.resolve("item_tag", tag.tagId)
            root.children[tag.key()] = category
        }
    }

    private fun buildByExclusive(root: TreeNodeModel, enchantments: List<ElementEntry<Enchantment>>) {
        val grouped = LinkedHashMap<String, MutableList<ElementEntry<Enchantment>>>()

        enchantments.forEach { entry ->
            val tagKey = entry.data().exclusiveSet().unwrapKey()
            if (tagKey.isPresent) {
                grouped.computeIfAbsent(tagKey.get().location().path) { ArrayList() }.add(entry)
                return@forEach
            }

            val directIds = entry.data().exclusiveSet().stream()
                .map { holder -> holder.unwrapKey().map { key -> key.identifier() }.orElse(null) }
                .filter { id -> id != null }
                .toList()

            directIds.forEach { identifier ->
                grouped.computeIfAbsent(identifier.toString()) { ArrayList() }.add(entry)
            }
        }

        grouped.forEach { (key, value) ->
            val category = createCategoryNode(value)
            val tagId = Identifier.tryParse(key)
            category.label = if (tagId != null) {
                StudioText.resolve("enchantment_tag", tagId)
            } else {
                key.lowercase(Locale.ROOT)
            }
            root.children[key] = category
        }
    }

    private fun createCategoryNode(enchantments: List<ElementEntry<Enchantment>>): TreeNodeModel {
        val node = TreeNodeModel()
        node.count = enchantments.size

        enchantments.forEach { entry ->
            val leaf = TreeNodeModel()
            leaf.elementId = entry.id().toString()
            leaf.label = entry.data().description().string
            node.children[entry.id().toString()] = leaf
        }

        return node
    }
}

package fr.hardel.asset_editor.client.compose.components.page.enchantment

import fr.hardel.asset_editor.client.memory.session.server.StudioDataSlots
import fr.hardel.asset_editor.studio.CompendiumTagEntry
import fr.hardel.asset_editor.studio.CompendiumTagGroup
import fr.hardel.asset_editor.tag.TagSeed
import net.minecraft.resources.Identifier

object EnchantmentTreeData {

    private val ENCHANTABLE_GROUP = Identifier.fromNamespaceAndPath("asset_editor", "enchantable")

    val ITEM_TAGS: List<ItemTagConfig>
        get() = CompendiumTagGroup.findEntries(StudioDataSlots.COMPENDIUM_ITEMS.memory().snapshot(), ENCHANTABLE_GROUP)
            .map { entry -> ItemTagConfig(entry.id(), toSeed(entry)) }

    private fun toSeed(entry: CompendiumTagEntry): TagSeed? {
        if (!entry.hasDefaults()) return null
        return TagSeed.fromValueLiterals(entry.defaults())
    }

    data class ItemTagConfig(val tagId: Identifier, val seed: TagSeed? = null) {
        fun key(): String = tagId.path
        fun icon(): Identifier = tagId.withPath("textures/studio/item/${tagId.path}.png")
    }
}

enum class StudioSidebarView {
    SLOTS, ITEMS, EXCLUSIVE
}
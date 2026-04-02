package fr.hardel.asset_editor.client.compose.lib.data

import fr.hardel.asset_editor.client.AssetEditorClient
import fr.hardel.asset_editor.studio.SuggestedTagEntry
import fr.hardel.asset_editor.tag.TagSeed
import net.minecraft.resources.Identifier

object EnchantmentTreeData {

    private val ENCHANTABLE_GROUP = Identifier.fromNamespaceAndPath("asset_editor", "enchantable")

    val ITEM_TAGS: List<ItemTagConfig>
        get() = AssetEditorClient.studioConfigMemory().snapshot()
            .itemEntriesFor(ENCHANTABLE_GROUP)
            .map { entry -> ItemTagConfig(entry.id(), toSeed(entry)) }

    private fun toSeed(entry: SuggestedTagEntry): TagSeed? {
        if (!entry.hasDefaults()) return null
        return TagSeed.fromValueLiterals(entry.defaults())
    }

    data class ItemTagConfig(val tagId: Identifier, val seed: TagSeed? = null) {
        fun key(): String = tagId.path
        fun icon(): Identifier = tagId.withPath("textures/studio/item/${tagId.path}.png")
    }
}

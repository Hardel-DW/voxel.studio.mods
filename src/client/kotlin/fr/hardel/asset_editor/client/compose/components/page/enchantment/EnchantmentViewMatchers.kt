package fr.hardel.asset_editor.client.compose.components.page.enchantment

import fr.hardel.asset_editor.client.compose.lib.SlotConfigs
import fr.hardel.asset_editor.client.compose.lib.StudioSidebarView
import fr.hardel.asset_editor.store.ElementEntry
import net.minecraft.world.item.enchantment.Enchantment
import java.util.Locale

object EnchantmentViewMatchers {

    fun interface ViewMatcher {
        fun matches(entry: ElementEntry<Enchantment>, category: String): Boolean
    }

    private val matchers = mapOf(
        StudioSidebarView.SLOTS to ViewMatcher(::matchesSlot),
        StudioSidebarView.ITEMS to ViewMatcher(::matchesItem),
        StudioSidebarView.EXCLUSIVE to ViewMatcher(::matchesExclusive)
    )

    @JvmStatic
    fun matches(entry: ElementEntry<Enchantment>, filterPath: String, sidebarView: StudioSidebarView): Boolean {
        if (filterPath.isEmpty()) {
            return true
        }

        val parts = filterPath.split("/", limit = 2)
        val category = parts[0]
        val leaf = if (parts.size == 2) parts[1] else ""
        if (leaf.isNotEmpty() && entry.id().path != leaf) {
            return false
        }

        val matcher = matchers[sidebarView]
        return matcher != null && matcher.matches(entry, category)
    }

    @JvmStatic
    fun matchesSlot(entry: ElementEntry<Enchantment>, category: String): Boolean {
        return entry.data().definition().slots()
            .any { group -> SlotConfigs.expandsTo(group.serializedName, category) }
    }

    @JvmStatic
    fun matchesItem(entry: ElementEntry<Enchantment>, category: String): Boolean {
        val supported = entry.data().definition().supportedItems().unwrapKey()
            .map { tag -> tag.location().path == category }
            .orElse(false)
        if (supported) {
            return true
        }

        val primary = entry.data().definition().primaryItems()
            .flatMap { holders -> holders.unwrapKey() }
            .map { tag -> tag.location().path == category }
            .orElse(false)
        if (primary) {
            return true
        }

        return entry.tags().stream().anyMatch { tag -> tag.path == category }
    }

    private fun matchesExclusive(entry: ElementEntry<Enchantment>, category: String): Boolean {
        val normalizedCategory = category.lowercase(Locale.ROOT)
        val tagKey = entry.data().exclusiveSet().unwrapKey()
        if (tagKey.isPresent) {
            val location = tagKey.get().location()
            val full = location.toString().lowercase(Locale.ROOT)
            val path = location.path.lowercase(Locale.ROOT)
            return full == normalizedCategory || path == normalizedCategory
        }

        return entry.data().exclusiveSet().stream()
            .map { holder -> holder.unwrapKey().map { key -> key.identifier() }.orElse(null) }
            .filter { id -> id != null }
            .anyMatch { id ->
                id.toString().lowercase(Locale.ROOT) == normalizedCategory ||
                    id.path.lowercase(Locale.ROOT) == normalizedCategory
            }
    }
}
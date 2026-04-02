package fr.hardel.asset_editor.client.compose.lib.data

import fr.hardel.asset_editor.client.AssetEditorClient
import net.minecraft.resources.Identifier

data class ExclusiveSetGroup(val tagId: Identifier) {

    fun id(): String = tagId.path

    fun image(): Identifier =
        tagId.withPath("textures/studio/enchantment/${tagId.path}.png")

    fun value(): String = "#$tagId"

    companion object {
        private val EXCLUSIVE_GROUP = Identifier.fromNamespaceAndPath("asset_editor", "exclusive")

        val ALL: List<ExclusiveSetGroup>
            get() = AssetEditorClient.studioConfigMemory().snapshot()
                .enchantmentEntriesFor(EXCLUSIVE_GROUP)
                .map { entry -> ExclusiveSetGroup(entry.id()) }
    }
}

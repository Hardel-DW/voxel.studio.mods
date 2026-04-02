package fr.hardel.asset_editor.client.navigation

import fr.hardel.asset_editor.client.compose.lib.data.StudioRegistryAccess
import net.minecraft.resources.Identifier

data class StudioEditorTab(
    val id: Identifier
) {
    fun path(): String = id.path
}

object StudioEditorTabs {
    @JvmStatic
    fun entries(): List<StudioEditorTab> =
        StudioRegistryAccess.editorTabs()

    @JvmStatic
    fun byId(id: Identifier): StudioEditorTab? =
        StudioRegistryAccess.editorTab(id)

    @JvmStatic
    fun require(id: Identifier): StudioEditorTab =
        byId(id) ?: error("Unknown studio editor tab '$id'")
}

package fr.hardel.asset_editor.client.compose.components.page.debug

import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioTranslation
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

val STUDIO_ITEMS_ATLAS_ID: Identifier =
    Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "studio_items")

data class AtlasOption(val id: Identifier, val label: String)

fun buildAtlasOptions(): List<AtlasOption> {
    val options = mutableListOf(
        AtlasOption(id = STUDIO_ITEMS_ATLAS_ID, label = I18n.get("debug:render.atlas.studio_items"))
    )
    Minecraft.getInstance().atlasManager.forEach { atlasId, _ ->
        options.add(AtlasOption(id = atlasId, label = StudioTranslation.resolve("debug:render.atlas", atlasId)))
    }
    return options
}

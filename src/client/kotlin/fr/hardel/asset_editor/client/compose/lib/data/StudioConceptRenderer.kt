package fr.hardel.asset_editor.client.compose.lib.data

import androidx.compose.runtime.Composable
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import net.minecraft.resources.Identifier

interface StudioConceptRenderer {
    val conceptId: Identifier
    val supportsSimulation: Boolean get() = false
    val shouldPrefetchAtlas: Boolean get() = false

    @Composable
    fun Render(context: StudioContext)
}

abstract class BaseStudioConceptRenderer(
    conceptPath: String,
    override val supportsSimulation: Boolean = false,
    override val shouldPrefetchAtlas: Boolean = false
) : StudioConceptRenderer {
    final override val conceptId: Identifier =
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, conceptPath)
}

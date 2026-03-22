package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.lib.assets.LocalStudioAssetCache
import net.minecraft.resources.Identifier

private val SHINE_LOCATION = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "textures/shine.png")

@Composable
fun ShineOverlay(
    modifier: Modifier = Modifier,
    opacity: Float = 0.15f
) {
    val bitmap = LocalStudioAssetCache.current.bitmap(SHINE_LOCATION) ?: return

    Image(
        bitmap = bitmap,
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = modifier
            .alpha(opacity)
    )
}

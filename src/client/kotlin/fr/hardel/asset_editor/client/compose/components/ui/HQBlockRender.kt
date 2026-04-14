package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import fr.hardel.asset_editor.client.compose.lib.rememberHighQualityBlockBitmap
import net.minecraft.resources.Identifier

@Composable
fun HQBlockRender(
    itemId: Identifier,
    displaySize: Dp,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val renderSize = with(density) { (displaySize.toPx() * 2f).toInt().coerceAtLeast(96) }
    val bitmap = rememberHighQualityBlockBitmap(itemId, renderSize)
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.High,
            modifier = modifier.size(displaySize)
        )
    } else {
        ItemSprite(itemId = itemId, displaySize = displaySize, modifier = modifier)
    }
}

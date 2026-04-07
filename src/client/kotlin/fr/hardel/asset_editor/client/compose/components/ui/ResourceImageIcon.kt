package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import fr.hardel.asset_editor.client.compose.lib.assets.LocalStudioAssetCache
import net.minecraft.resources.Identifier
import kotlin.math.roundToInt

@Composable
fun ResourceImageIcon(
    location: Identifier,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val bitmap = LocalStudioAssetCache.current.bitmap(location) ?: return

    Canvas(modifier = modifier.size(size)) {
        drawImage(
            image = bitmap,
            dstSize = IntSize(this.size.width.roundToInt(), this.size.height.roundToInt()),
            filterQuality = FilterQuality.None
        )
    }
}

package fr.hardel.asset_editor.client.compose.components.page.recipe

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import fr.hardel.asset_editor.client.compose.lib.assets.LocalStudioAssetCache
import kotlin.math.min
import kotlin.math.roundToInt
import net.minecraft.resources.Identifier

@Composable
fun RecipeGuiAsset(
    location: Identifier,
    width: Int,
    height: Int,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val bitmap = LocalStudioAssetCache.current.bitmap(location) ?: return

    Canvas(modifier = modifier.size(size)) {
        val scale = min(this.size.width / width.toFloat(), this.size.height / height.toFloat())
        val targetWidth = (width * scale).roundToInt()
        val targetHeight = (height * scale).roundToInt()
        val offsetX = ((this.size.width - targetWidth) / 2f).roundToInt()
        val offsetY = ((this.size.height - targetHeight) / 2f).roundToInt()

        drawImage(
            image = bitmap,
            dstOffset = IntOffset(offsetX, offsetY),
            dstSize = IntSize(targetWidth, targetHeight),
            filterQuality = FilterQuality.None
        )
    }
}

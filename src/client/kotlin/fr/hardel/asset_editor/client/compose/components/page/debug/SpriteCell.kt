package fr.hardel.asset_editor.client.compose.components.page.debug

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import kotlin.math.roundToInt

private val CELL_PADDING: Dp = 4.dp

@Composable
fun SpriteCell(
    atlasImage: ImageBitmap,
    sourceX: Int,
    sourceY: Int,
    sourceWidth: Int,
    sourceHeight: Int,
    rowHeight: Dp
) {
    if (sourceWidth <= 0 || sourceHeight <= 0) {
        return
    }

    val aspectRatio = sourceWidth.toFloat() / sourceHeight.toFloat()
    val displayHeight = rowHeight
    val displayWidth = rowHeight * aspectRatio

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(displayWidth + CELL_PADDING * 2)
            .height(displayHeight + CELL_PADDING * 2)
            .background(StudioColors.Card, RoundedCornerShape(4.dp))
            .padding(CELL_PADDING)
    ) {
        Canvas(
            modifier = Modifier
                .width(displayWidth)
                .height(displayHeight)
        ) {
            drawImage(
                image = atlasImage,
                srcOffset = IntOffset(sourceX, sourceY),
                srcSize = IntSize(sourceWidth, sourceHeight),
                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                filterQuality = FilterQuality.None
            )
        }
    }
}

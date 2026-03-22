package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import fr.hardel.asset_editor.client.compose.lib.assets.LocalStudioAssetCache
import net.minecraft.resources.Identifier
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.ColorFilter
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.svg.SVGDOM

@Composable
fun SvgIcon(
    location: Identifier,
    size: Dp,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val document = LocalStudioAssetCache.current.svg(location)?.document ?: return

    Canvas(modifier = modifier.size(size)) {
        renderSvgDocument(
            document = document,
            tint = tint,
            canvasSize = this.size
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.renderSvgDocument(
    document: SVGDOM,
    tint: Color,
    canvasSize: Size
) {
    drawIntoCanvas { canvas ->
        val nativeCanvas = canvas.nativeCanvas
        val tintFilter = ColorFilter.makeBlend(tint.toArgb(), BlendMode.SRC_IN)
        val layerPaint = Paint()

        try {
            layerPaint.colorFilter = tintFilter
            nativeCanvas.saveLayer(Rect.makeWH(canvasSize.width, canvasSize.height), layerPaint)
            document.setContainerSize(canvasSize.width, canvasSize.height)
            document.render(nativeCanvas)
            nativeCanvas.restore()
        } finally {
            layerPaint.close()
            tintFilter.close()
        }
    }
}

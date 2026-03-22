package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import fr.hardel.asset_editor.client.javafx.VoxelResourceLoader
import net.minecraft.resources.Identifier
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.ColorFilter
import org.jetbrains.skia.Data
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.svg.SVGDOM
import org.jetbrains.skia.svg.SVGLength
import org.jetbrains.skia.svg.SVGLengthUnit
import org.jetbrains.skia.svg.SVGPreserveAspectRatio
import org.jetbrains.skia.svg.SVGPreserveAspectRatioAlign
import org.jetbrains.skia.svg.SVGPreserveAspectRatioScale
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("SvgIcon")

private fun loadSvgDocument(location: Identifier): SVGDOM? = try {
    VoxelResourceLoader.open(location).use { stream ->
        SVGDOM(Data.makeFromBytes(stream.readBytes())).apply {
            root?.apply {
                x = SVGLength(0f)
                y = SVGLength(0f)
                width = SVGLength(100f, SVGLengthUnit.PERCENTAGE)
                height = SVGLength(100f, SVGLengthUnit.PERCENTAGE)
                preserveAspectRatio = SVGPreserveAspectRatio(
                    SVGPreserveAspectRatioAlign.XMID_YMID,
                    SVGPreserveAspectRatioScale.MEET
                )
            }
        }
    }
} catch (exception: Exception) {
    logger.warn("Failed to load SVG {}: {}", location, exception.message)
    null
}

@Composable
fun SvgIcon(
    location: Identifier,
    size: Dp,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val document = remember(location) { loadSvgDocument(location) } ?: return

    DisposableEffect(document) {
        onDispose {
            document.close()
        }
    }

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

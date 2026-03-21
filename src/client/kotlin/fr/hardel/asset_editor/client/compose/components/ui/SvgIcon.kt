package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.Dp
import fr.hardel.asset_editor.client.javafx.VoxelResourceLoader
import net.minecraft.resources.Identifier
import org.jetbrains.skia.Data
import org.jetbrains.skia.Surface
import org.jetbrains.skia.svg.SVGDOM


private fun renderSvg(location: Identifier, sizePx: Int): org.jetbrains.skia.Image? = try {
    val bytes = VoxelResourceLoader.open(location).use { it.readBytes() }
    val dom = SVGDOM(Data.makeFromBytes(bytes))
    dom.setContainerSize(sizePx.toFloat(), sizePx.toFloat())

    val surface = Surface.makeRasterN32Premul(sizePx, sizePx)
    dom.render(surface.canvas)
    surface.makeImageSnapshot()
} catch (_: Exception) {
    null
}

@Composable
fun SvgIcon(
    location: Identifier,
    size: Dp,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val sizePx = size.value.toInt().coerceAtLeast(1)
    val bitmap = remember(location, sizePx) {
        renderSvg(location, sizePx)?.toComposeImageBitmap()
    } ?: return

    Image(
        painter = BitmapPainter(bitmap),
        contentDescription = null,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier.size(size)
    )
}

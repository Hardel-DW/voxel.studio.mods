package fr.hardel.asset_editor.client.compose.lib.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

fun argbPixelsToImageBitmap(argbPixels: IntArray, width: Int, height: Int): ImageBitmap {
    val pixels = ByteArray(width * height * 4)
    for (index in argbPixels.indices) {
        val argb = argbPixels[index]
        val offset = index * 4
        pixels[offset] = (argb and 0xFF).toByte()
        pixels[offset + 1] = ((argb shr 8) and 0xFF).toByte()
        pixels[offset + 2] = ((argb shr 16) and 0xFF).toByte()
        pixels[offset + 3] = ((argb shr 24) and 0xFF).toByte()
    }
    val info = ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL)
    return Image.makeRaster(info, pixels, width * 4).toComposeImageBitmap()
}

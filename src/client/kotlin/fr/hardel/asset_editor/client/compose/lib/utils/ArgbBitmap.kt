package fr.hardel.asset_editor.client.compose.lib.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.util.concurrent.ConcurrentLinkedQueue
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

private const val POOL_MAX_ENTRIES = 4

private val bytePool = ConcurrentLinkedQueue<ByteArray>()

private fun borrowBytes(size: Int): ByteArray {
    while (true) {
        val buf = bytePool.poll() ?: return ByteArray(size)
        if (buf.size == size) return buf
    }
}

private fun releaseBytes(buf: ByteArray) {
    if (bytePool.size < POOL_MAX_ENTRIES) bytePool.offer(buf)
}

fun argbPixelsToImageBitmap(argbPixels: IntArray, width: Int, height: Int): ImageBitmap {
    val byteCount = width * height * 4
    val pixels = borrowBytes(byteCount)
    try {
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
    } finally {
        releaseBytes(pixels)
    }
}

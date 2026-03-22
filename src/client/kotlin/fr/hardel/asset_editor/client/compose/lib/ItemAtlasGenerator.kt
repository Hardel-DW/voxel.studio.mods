package fr.hardel.asset_editor.client.compose.lib

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import fr.hardel.asset_editor.client.rendering.ItemAtlasRenderer
import fr.hardel.asset_editor.client.rendering.ItemAtlasRenderer.AtlasEntry
import java.util.concurrent.CopyOnWriteArrayList
import javax.swing.SwingUtilities
import net.minecraft.resources.Identifier
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

object ItemAtlasGenerator {

    private val listeners = CopyOnWriteArrayList<Runnable>()

    @Volatile
    private var atlasImage: ImageBitmap? = null

    init {
        ItemAtlasRenderer.subscribeGeneration(::rebuild)
    }

    @JvmStatic
    fun getAtlasImage(): ImageBitmap? {
        if (atlasImage == null && ItemAtlasRenderer.isReady()) {
            rebuild()
        }
        return atlasImage
    }

    @JvmStatic
    fun getEntry(itemId: Identifier): AtlasEntry? =
        ItemAtlasRenderer.getEntry(itemId)

    @JvmStatic
    fun isReady(): Boolean =
        ItemAtlasRenderer.isReady()

    @JvmStatic
    fun subscribe(listener: Runnable): Runnable {
        listeners.add(listener)
        return Runnable { listeners.remove(listener) }
    }

    @JvmStatic
    fun rebuild() {
        val pixels = ItemAtlasRenderer.getArgbPixels() ?: return
        val width = ItemAtlasRenderer.getWidth()
        val height = ItemAtlasRenderer.getHeight()
        val update = Runnable {
            atlasImage = createImage(pixels, width, height)
            listeners.forEach(Runnable::run)
        }

        if (SwingUtilities.isEventDispatchThread()) {
            update.run()
        } else {
            SwingUtilities.invokeLater(update)
        }
    }

    private fun createImage(argbPixels: IntArray, width: Int, height: Int): ImageBitmap {
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
}

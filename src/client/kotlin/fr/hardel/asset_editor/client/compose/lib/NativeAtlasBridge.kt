package fr.hardel.asset_editor.client.compose.lib

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import fr.hardel.asset_editor.client.rendering.NativeAtlasSnapshotService
import net.minecraft.resources.Identifier
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import javax.swing.SwingUtilities

object NativeAtlasBridge {

    private val listeners = CopyOnWriteArrayList<Runnable>()

    @Volatile
    private var cachedAtlasId: Identifier? = null

    @Volatile
    private var cachedImage: ImageBitmap? = null

    @Volatile
    private var cachedSnapshot: NativeAtlasSnapshotService.Snapshot? = null

    init {
        NativeAtlasSnapshotService.subscribe(::onSnapshotReady)
    }

    fun request(atlasId: Identifier) {
        NativeAtlasSnapshotService.requestSnapshot(atlasId)
    }

    fun getImage(): ImageBitmap? = cachedImage

    fun getSnapshot(): NativeAtlasSnapshotService.Snapshot? = cachedSnapshot

    fun subscribe(listener: Runnable): Runnable {
        listeners.add(listener)
        return Runnable { listeners.remove(listener) }
    }

    private fun onSnapshotReady() {
        val snapshot = NativeAtlasSnapshotService.getSnapshot() ?: return

        CompletableFuture
            .supplyAsync { createImage(snapshot.argbPixels(), snapshot.width(), snapshot.height()) }
            .thenAccept { image ->
                val publish = Runnable {
                    cachedAtlasId = snapshot.atlasId()
                    cachedImage = image
                    cachedSnapshot = snapshot
                    listeners.forEach(Runnable::run)
                }
                if (SwingUtilities.isEventDispatchThread()) publish.run()
                else SwingUtilities.invokeLater(publish)
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

package fr.hardel.asset_editor.client.compose.lib

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import fr.hardel.asset_editor.client.compose.lib.utils.argbPixelsToImageBitmap
import fr.hardel.asset_editor.client.rendering.HighQualityBlockRenderer
import java.util.Collections
import javax.swing.SwingUtilities
import net.minecraft.resources.Identifier

private const val BITMAP_CACHE_CAPACITY = 256

object HighQualityBlockCache {

    private val bitmapCache: MutableMap<HighQualityBlockRenderer.Key, ImageBitmap> = Collections.synchronizedMap(
        object : java.util.LinkedHashMap<HighQualityBlockRenderer.Key, ImageBitmap>(BITMAP_CACHE_CAPACITY + 1, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<HighQualityBlockRenderer.Key, ImageBitmap>): Boolean =
                size > BITMAP_CACHE_CAPACITY
        }
    )
    private val listeners = java.util.concurrent.CopyOnWriteArrayList<(HighQualityBlockRenderer.Key) -> Unit>()

    init {
        HighQualityBlockRenderer.subscribe { key ->
            val result = HighQualityBlockRenderer.getResult(key) ?: return@subscribe
            val bitmap = argbPixelsToImageBitmap(result.argbPixels(), result.width(), result.height())
            bitmapCache[key] = bitmap
            val publish = Runnable { listeners.forEach { it(key) } }
            if (SwingUtilities.isEventDispatchThread()) publish.run() else SwingUtilities.invokeLater(publish)
        }
    }

    fun getOrRequest(itemId: Identifier, size: Int): ImageBitmap? {
        val key = HighQualityBlockRenderer.Key(itemId, size)
        bitmapCache[key]?.let { return it }
        HighQualityBlockRenderer.request(itemId, size)
        return null
    }

    fun subscribe(listener: (HighQualityBlockRenderer.Key) -> Unit): Runnable {
        listeners.add(listener)
        return Runnable { listeners.remove(listener) }
    }

    @JvmStatic
    fun dispose() {
        bitmapCache.clear()
        HighQualityBlockRenderer.dispose()
    }
}

@Composable
fun rememberHighQualityBlockBitmap(itemId: Identifier, size: Int): ImageBitmap? {
    var bitmap by remember(itemId, size) { mutableStateOf<ImageBitmap?>(null) }
    DisposableEffect(itemId, size) {
        bitmap = HighQualityBlockCache.getOrRequest(itemId, size)
        val target = HighQualityBlockRenderer.Key(itemId, size)
        val sub = HighQualityBlockCache.subscribe { completed ->
            if (completed == target) {
                bitmap = HighQualityBlockCache.getOrRequest(itemId, size)
            }
        }
        onDispose(sub::run)
    }
    return bitmap
}

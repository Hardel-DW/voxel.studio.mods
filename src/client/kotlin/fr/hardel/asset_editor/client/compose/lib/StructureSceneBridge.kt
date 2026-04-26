package fr.hardel.asset_editor.client.compose.lib

import androidx.compose.ui.graphics.ImageBitmap
import fr.hardel.asset_editor.client.compose.lib.utils.argbPixelsToImageBitmap
import fr.hardel.asset_editor.client.rendering.StructureSceneRenderer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import javax.swing.SwingUtilities

/**
 * Bridges the off-screen [StructureSceneRenderer] (Minecraft render thread) and Compose UI
 * (Swing EDT). Each renderer readback flows through here, gets converted to an
 * [ImageBitmap] off-EDT, then handed to subscribers on EDT.
 *
 * Decoded images are cached by full key (camera + viewport included) so that panning back to a
 * recent angle is a zero-render hit. The cache is bounded by [MAX_IMAGES] in insertion order.
 */
object StructureSceneBridge {
    private const val MAX_IMAGES = 6

    private val listeners = CopyOnWriteArrayList<(String) -> Unit>()
    private val images = ConcurrentHashMap<String, ImageBitmap>()
    private val imageOrder = ConcurrentLinkedQueue<String>()

    init {
        StructureSceneRenderer.subscribe(::onSceneReady)
    }

    fun request(request: StructureSceneRenderer.Request) {
        StructureSceneRenderer.request(request)
    }

    fun getImage(key: String): ImageBitmap? = images[key]

    fun subscribe(listener: (String) -> Unit): Runnable {
        listeners.add(listener)
        return Runnable { listeners.remove(listener) }
    }

    private fun onSceneReady(key: String) {
        val result = StructureSceneRenderer.getResult(key) ?: return
        CompletableFuture
            .supplyAsync { argbPixelsToImageBitmap(result.argbPixels(), result.width(), result.height()) }
            .thenAccept { image ->
                val publish = Runnable {
                    if (images.put(key, image) == null) imageOrder.add(key)
                    trimImages()
                    listeners.forEach { it(key) }
                }
                if (SwingUtilities.isEventDispatchThread()) publish.run() else SwingUtilities.invokeLater(publish)
            }
            .exceptionally { null }
    }

    private fun trimImages() {
        while (images.size > MAX_IMAGES) {
            val oldest = imageOrder.poll() ?: return
            images.remove(oldest)
        }
    }
}

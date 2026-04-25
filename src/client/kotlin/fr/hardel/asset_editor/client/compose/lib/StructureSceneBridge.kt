package fr.hardel.asset_editor.client.compose.lib

import androidx.compose.ui.graphics.ImageBitmap
import fr.hardel.asset_editor.client.compose.lib.utils.argbPixelsToImageBitmap
import fr.hardel.asset_editor.client.rendering.StructureSceneRenderer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import javax.swing.SwingUtilities

object StructureSceneBridge {
    private const val MAX_IMAGES = 6

    private val listeners = CopyOnWriteArrayList<(String) -> Unit>()
    private val images = ConcurrentHashMap<String, ImageBitmap>()
    private val imageOrder = ConcurrentLinkedQueue<String>()
    private val pending = ConcurrentHashMap.newKeySet<String>()

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
        if (!pending.add(key)) return
        val result = StructureSceneRenderer.getResult(key) ?: run {
            pending.remove(key)
            return
        }
        CompletableFuture
            .supplyAsync { argbPixelsToImageBitmap(result.argbPixels(), result.width(), result.height()) }
            .thenAccept { image ->
                val publish = Runnable {
                    if (images.put(key, image) == null) {
                        imageOrder.add(key)
                    }
                    trimImages()
                    pending.remove(key)
                    listeners.forEach { it(key) }
                }
                if (SwingUtilities.isEventDispatchThread()) publish.run() else SwingUtilities.invokeLater(publish)
            }
            .exceptionally {
                pending.remove(key)
                null
            }
    }

    private fun trimImages() {
        while (images.size > MAX_IMAGES) {
            val oldest = imageOrder.poll() ?: return
            images.remove(oldest)
        }
    }
}

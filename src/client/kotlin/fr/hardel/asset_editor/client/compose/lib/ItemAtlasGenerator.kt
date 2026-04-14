package fr.hardel.asset_editor.client.compose.lib

import androidx.compose.ui.graphics.ImageBitmap
import fr.hardel.asset_editor.client.compose.lib.utils.argbPixelsToImageBitmap
import fr.hardel.asset_editor.client.rendering.ItemAtlasRenderer
import fr.hardel.asset_editor.client.rendering.ItemAtlasRenderer.AtlasEntry
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import javax.swing.SwingUtilities
import net.minecraft.resources.Identifier

object ItemAtlasGenerator {

    private val listeners = CopyOnWriteArrayList<Runnable>()
    private val rebuildLock = Any()

    @Volatile
    private var atlasImage: ImageBitmap? = null

    @Volatile
    private var materializedGeneration: Long = -1L

    @Volatile
    private var pendingGeneration: Long = -1L

    init {
        ItemAtlasRenderer.subscribeGeneration(::rebuild)
    }

    @JvmStatic
    fun getAtlasImage(): ImageBitmap? {
        if (!ItemAtlasRenderer.isReady()) {
            ItemAtlasRenderer.requestGeneration()
            return atlasImage
        }
        if (ItemAtlasRenderer.isReady() && ItemAtlasRenderer.getGeneration() > materializedGeneration) {
            rebuild()
        }
        return atlasImage
    }

    @JvmStatic
    fun getEntry(itemId: Identifier): AtlasEntry? {
        if (!ItemAtlasRenderer.isReady()) {
            ItemAtlasRenderer.requestGeneration()
        }
        return ItemAtlasRenderer.getEntry(itemId)
    }

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
        val generation = ItemAtlasRenderer.getGeneration()

        synchronized(rebuildLock) {
            if (generation <= materializedGeneration || generation == pendingGeneration) {
                return
            }
            pendingGeneration = generation
        }

        CompletableFuture
            .supplyAsync { argbPixelsToImageBitmap(pixels, width, height) }
            .thenAccept { image ->
                val publish = Runnable {
                    synchronized(rebuildLock) {
                        if (generation < materializedGeneration) {
                            return@Runnable
                        }
                        atlasImage = image
                        materializedGeneration = generation
                        if (pendingGeneration == generation) {
                            pendingGeneration = -1L
                        }
                    }
                    listeners.forEach(Runnable::run)
                }

                if (SwingUtilities.isEventDispatchThread()) {
                    publish.run()
                } else {
                    SwingUtilities.invokeLater(publish)
                }
            }
            .exceptionally {
                synchronized(rebuildLock) {
                    if (pendingGeneration == generation) {
                        pendingGeneration = -1L
                    }
                }
                null
            }
    }

}

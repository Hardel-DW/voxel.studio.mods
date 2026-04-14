package fr.hardel.asset_editor.client.compose.lib

import androidx.compose.ui.graphics.ImageBitmap
import fr.hardel.asset_editor.client.compose.lib.utils.argbPixelsToImageBitmap
import fr.hardel.asset_editor.client.rendering.NativeAtlasSnapshotService
import net.minecraft.resources.Identifier
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
            .supplyAsync { argbPixelsToImageBitmap(snapshot.argbPixels(), snapshot.width(), snapshot.height()) }
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
}

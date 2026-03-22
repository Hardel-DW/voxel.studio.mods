package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import fr.hardel.asset_editor.client.resource.StudioResourceLoader
import net.minecraft.resources.Identifier
import org.jetbrains.skia.Image as SkiaImage
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

private val resourceImageLogger = LoggerFactory.getLogger("ResourceImageIcon")
private val resourceImageCache = ConcurrentHashMap<String, ImageBitmap>()
private val missingResourceImages = ConcurrentHashMap.newKeySet<String>()

private fun loadResourceImage(location: Identifier, size: Dp): ImageBitmap? {
    val resourceKey = location.toString()
    val cacheKey = "$resourceKey@${size.value}"
    if (missingResourceImages.contains(resourceKey)) {
        return null
    }

    resourceImageCache[cacheKey]?.let { return it }

    return try {
        val bitmap = StudioResourceLoader.open(location).use { stream ->
            SkiaImage.makeFromEncoded(stream.readBytes()).toComposeImageBitmap()
        }
        resourceImageCache[cacheKey] = bitmap
        bitmap
    } catch (exception: Exception) {
        resourceImageLogger.warn("Failed to load resource image {}: {}", location, exception.message)
        missingResourceImages += resourceKey
        null
    }
}

@Composable
fun ResourceImageIcon(
    location: Identifier,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(location, size) { loadResourceImage(location, size) } ?: return

    Image(
        bitmap = bitmap,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size)
    )
}

package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.resource.StudioResourceLoader
import net.minecraft.resources.Identifier
import org.jetbrains.skia.Image as SkiaImage

private val SHINE_LOCATION = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "textures/shine.png")

private fun loadShineBitmap(): ImageBitmap? = try {
    val bytes = StudioResourceLoader.open(SHINE_LOCATION).use { it.readBytes() }
    SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
} catch (_: Exception) {
    null
}

@Composable
fun ShineOverlay(
    modifier: Modifier = Modifier,
    opacity: Float = 0.15f
) {
    val bitmap = remember { loadShineBitmap() } ?: return

    Image(
        bitmap = bitmap,
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = modifier
            .alpha(opacity)
    )
}

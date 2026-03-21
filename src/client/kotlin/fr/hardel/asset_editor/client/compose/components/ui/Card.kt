package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.javafx.VoxelResourceLoader
import org.jetbrains.skia.Image as SkiaImage
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import net.minecraft.resources.Identifier

private val CHECK_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/check.svg")
private val LOCK_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/tools/lock.svg")

@Composable
fun Card(
    imageId: Identifier,
    title: String,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    locked: Boolean = false,
    lockText: String? = null
) {
    SimpleCard(
        padding = PaddingValues(16.dp),
        onClick = if (!locked) ({ onActiveChange(!active) }) else null,
        modifier = modifier.alpha(if (locked) 0.5f else 1f),
        overlay = {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                if (locked) {
                    SvgIcon(LOCK_ICON, 24.dp, Color.White, Modifier.align(Alignment.TopEnd))
                    if (lockText != null) {
                        Text(
                            text = lockText,
                            style = VoxelTypography.light(11),
                            color = VoxelColors.Zinc400,
                            modifier = Modifier.align(Alignment.BottomEnd)
                        )
                    }
                } else if (active) {
                    SvgIcon(CHECK_ICON, 24.dp, Color.White, Modifier.align(Alignment.TopEnd))
                }
            }
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                style = VoxelTypography.semiBold(16),
                color = VoxelColors.Zinc100
            )
            if (description != null) {
                Text(
                    text = description,
                    style = VoxelTypography.regular(13),
                    color = VoxelColors.Zinc400
                )
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp)
        ) {
            val bitmap = remember(imageId) {
                try {
                    val bytes = VoxelResourceLoader.open(imageId).use { it.readBytes() }
                    SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
                } catch (_: Exception) { null }
            }
            bitmap?.let {
                Image(
                    bitmap = it,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.None,
                    modifier = Modifier.size(64.dp)
                )
            }
        }
    }
}

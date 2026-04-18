package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.Dp
import fr.hardel.asset_editor.client.compose.lib.ItemAtlasGenerator
import kotlin.math.roundToInt
import net.minecraft.resources.Identifier

@Composable
fun ItemSprite(
    itemId: Identifier,
    displaySize: Dp,
    modifier: Modifier = Modifier
) {
    val atlas by ItemAtlasGenerator.atlasImageState
    val currentGeneration by ItemAtlasGenerator.generation

    LaunchedEffect(atlas == null) {
        if (atlas == null) ItemAtlasGenerator.getAtlasImage()
    }

    val image = atlas ?: return
    val entry = remember(itemId, currentGeneration) { ItemAtlasGenerator.getEntry(itemId) } ?: return

    Canvas(
        modifier = modifier.size(displaySize)
    ) {
        drawImage(
            image = image,
            srcOffset = androidx.compose.ui.unit.IntOffset(entry.x(), entry.y()),
            srcSize = androidx.compose.ui.unit.IntSize(entry.size(), entry.size()),
            dstSize = androidx.compose.ui.unit.IntSize(size.width.roundToInt(), size.height.roundToInt()),
            filterQuality = FilterQuality.None
        )
    }
}

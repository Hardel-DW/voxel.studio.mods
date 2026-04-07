package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    var version by remember(itemId) { mutableIntStateOf(0) }

    DisposableEffect(itemId) {
        val subscription = ItemAtlasGenerator.subscribe { version++ }
        onDispose(subscription::run)
    }

    val atlas = remember(version) { ItemAtlasGenerator.getAtlasImage() }
    val entry = remember(itemId, version) { ItemAtlasGenerator.getEntry(itemId) }
    if (atlas == null || entry == null) {
        return
    }

    Canvas(
        modifier = modifier.size(displaySize)
    ) {
        drawImage(
            image = atlas,
            srcOffset = androidx.compose.ui.unit.IntOffset(entry.x(), entry.y()),
            srcSize = androidx.compose.ui.unit.IntSize(entry.size(), entry.size()),
            dstSize = androidx.compose.ui.unit.IntSize(size.width.roundToInt(), size.height.roundToInt()),
            filterQuality = FilterQuality.None
        )
    }
}

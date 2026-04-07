package fr.hardel.asset_editor.client.compose.components.page.debug

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.components.ui.ItemSprite
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.resources.Identifier
import kotlin.math.roundToInt

@Composable
fun ItemCell(itemId: Identifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .background(StudioColors.Card, RoundedCornerShape(4.dp))
    ) {
        ItemSprite(itemId, 32.dp)
    }
}

@Composable
fun SpriteCell(atlasImage: ImageBitmap, sprite: TextureAtlasSprite) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .background(StudioColors.Card, RoundedCornerShape(4.dp))
    ) {
        Canvas(modifier = Modifier.size(32.dp)) {
            drawImage(
                image = atlasImage,
                srcOffset = IntOffset(sprite.x, sprite.y),
                srcSize = IntSize(sprite.contents().width(), sprite.contents().height()),
                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                filterQuality = FilterQuality.None
            )
        }
    }
}

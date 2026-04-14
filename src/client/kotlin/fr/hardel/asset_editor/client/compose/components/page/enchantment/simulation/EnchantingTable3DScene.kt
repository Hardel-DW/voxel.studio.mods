package fr.hardel.asset_editor.client.compose.components.page.enchantment.simulation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.components.ui.ItemSprite
import net.minecraft.resources.Identifier

private data class BlockPos(val x: Int, val y: Int, val z: Int)

private val ENCHANTING_TABLE_POS = BlockPos(x = 2, y = 0, z = 2)

private val BOOKSHELF_ORDER = listOf(
    BlockPos(1, 0, 0), BlockPos(2, 0, 0), BlockPos(3, 0, 0),
    BlockPos(0, 0, 1), BlockPos(0, 0, 2), BlockPos(0, 0, 3),
    BlockPos(4, 0, 1), BlockPos(4, 0, 2), BlockPos(4, 0, 3),
    BlockPos(1, 1, 0), BlockPos(2, 1, 0), BlockPos(3, 1, 0),
    BlockPos(4, 1, 1), BlockPos(4, 1, 2),
    BlockPos(0, 1, 1)
)

private val RENDER_ORDER: List<Pair<BlockPos, Boolean>> = buildList {
    add(ENCHANTING_TABLE_POS to true)
    BOOKSHELF_ORDER.forEach { add(it to false) }
}.sortedWith(compareBy({ it.first.x + it.first.z }, { it.first.y }))

private val TILE_HALF_W = 16f
private val TILE_HALF_H = 8f
private val TILE_HEIGHT = 18f
private val BLOCK_SIZE = 36.dp
private val FALL_HEIGHT = 60.dp

private val TABLE_ITEM = Identifier.fromNamespaceAndPath("minecraft", "enchanting_table")
private val BOOKSHELF_ITEM = Identifier.fromNamespaceAndPath("minecraft", "bookshelf")

@Composable
fun EnchantingTable3DScene(
    bookshelves: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        for ((pos, isTable) in RENDER_ORDER) {
            val visible = isTable || BOOKSHELF_ORDER.indexOf(pos) < bookshelves
            AnimatedBlock(
                pos = pos,
                visible = visible,
                itemId = if (isTable) TABLE_ITEM else BOOKSHELF_ITEM
            )
        }
    }
}

@Composable
private fun AnimatedBlock(pos: BlockPos, visible: Boolean, itemId: Identifier) {
    val baseXDp = ((pos.x - pos.z) * TILE_HALF_W).dp
    val baseYDp = ((pos.x + pos.z) * TILE_HALF_H - pos.y * TILE_HEIGHT).dp
    val targetY = if (visible) baseYDp else baseYDp - FALL_HEIGHT
    val animatedY by animateDpAsState(
        targetValue = targetY,
        animationSpec = tween(durationMillis = 260)
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 220)
    )
    Box(
        modifier = Modifier
            .offset { IntOffset(baseXDp.roundToPx(), animatedY.roundToPx()) }
            .graphicsLayer { alpha = animatedAlpha }
    ) {
        ItemSprite(itemId = itemId, displaySize = BLOCK_SIZE)
    }
}

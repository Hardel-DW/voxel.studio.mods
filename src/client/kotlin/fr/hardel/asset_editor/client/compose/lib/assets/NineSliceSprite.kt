package fr.hardel.asset_editor.client.compose.lib.assets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.google.gson.JsonParser
import fr.hardel.asset_editor.client.compose.StudioResourceLoader
import java.util.concurrent.ConcurrentHashMap
import net.minecraft.resources.Identifier
import org.jetbrains.skia.Image as SkiaImage

data class NineSliceBorder(val left: Int, val right: Int, val top: Int, val bottom: Int) {
    companion object {
        val NONE = NineSliceBorder(0, 0, 0, 0)
    }
}

data class NineSliceSprite(
    val bitmap: ImageBitmap,
    val spriteWidth: Int,
    val spriteHeight: Int,
    val border: NineSliceBorder,
    val stretchInner: Boolean
)

object NineSliceSpriteLoader {

    private val cache = ConcurrentHashMap<Identifier, NineSliceSprite>()
    private val missing = ConcurrentHashMap.newKeySet<Identifier>()

    fun load(spriteId: Identifier): NineSliceSprite? {
        cache[spriteId]?.let { return it }
        if (spriteId in missing) return null
        val sprite = loadFromResources(spriteId)
        if (sprite == null) {
            missing += spriteId
            return null
        }
        cache[spriteId] = sprite
        return sprite
    }

    private fun loadFromResources(spriteId: Identifier): NineSliceSprite? {
        val texture = Identifier.fromNamespaceAndPath(spriteId.namespace, "textures/gui/sprites/${spriteId.path}.png")
        val mcmeta = Identifier.fromNamespaceAndPath(spriteId.namespace, "textures/gui/sprites/${spriteId.path}.png.mcmeta")

        val bitmap = runCatching {
            StudioResourceLoader.open(texture).use { stream ->
                SkiaImage.makeFromEncoded(stream.readBytes()).toComposeImageBitmap()
            }
        }.getOrNull() ?: return null

        val meta = runCatching {
            StudioResourceLoader.open(mcmeta).use { stream ->
                parseMcmeta(stream.readBytes().toString(Charsets.UTF_8))
            }
        }.getOrNull()

        val border = meta?.border ?: NineSliceBorder.NONE
        val spriteWidth = meta?.width ?: bitmap.width
        val spriteHeight = meta?.height ?: bitmap.height
        val stretchInner = meta?.stretchInner ?: true
        return NineSliceSprite(bitmap, spriteWidth, spriteHeight, border, stretchInner)
    }

    private data class McmetaData(val width: Int, val height: Int, val border: NineSliceBorder, val stretchInner: Boolean)

    private fun parseMcmeta(text: String): McmetaData? {
        val root = JsonParser.parseString(text).asJsonObject
        val gui = root.getAsJsonObject("gui") ?: return null
        val scaling = gui.getAsJsonObject("scaling") ?: return null
        if (scaling.get("type")?.asString != "nine_slice") return null

        val width = scaling.get("width")?.asInt ?: return null
        val height = scaling.get("height")?.asInt ?: return null
        val stretchInner = scaling.get("stretch_inner")?.asBoolean ?: false
        val border = parseBorder(scaling.get("border")) ?: return null
        return McmetaData(width, height, border, stretchInner)
    }

    private fun parseBorder(element: com.google.gson.JsonElement?): NineSliceBorder? {
        element ?: return null
        if (element.isJsonPrimitive) {
            val size = element.asInt
            return NineSliceBorder(size, size, size, size)
        }
        if (element.isJsonObject) {
            val obj = element.asJsonObject
            return NineSliceBorder(
                left = obj.get("left")?.asInt ?: 0,
                right = obj.get("right")?.asInt ?: 0,
                top = obj.get("top")?.asInt ?: 0,
                bottom = obj.get("bottom")?.asInt ?: 0
            )
        }
        return null
    }
}

@Composable
fun rememberNineSliceSprite(spriteId: Identifier): NineSliceSprite? =
    remember(spriteId) { NineSliceSpriteLoader.load(spriteId) }

fun DrawScope.drawNineSlice(sprite: NineSliceSprite, targetSize: IntSize, pixelScale: Int = 1) {
    val b = sprite.border
    val srcW = sprite.spriteWidth
    val srcH = sprite.spriteHeight
    val dstW = targetSize.width
    val dstH = targetSize.height
    if (dstW <= 0 || dstH <= 0) return

    val leftW = (b.left * pixelScale).dp.roundToPx().coerceAtMost(dstW / 2)
    val rightW = (b.right * pixelScale).dp.roundToPx().coerceAtMost(dstW / 2)
    val topH = (b.top * pixelScale).dp.roundToPx().coerceAtMost(dstH / 2)
    val bottomH = (b.bottom * pixelScale).dp.roundToPx().coerceAtMost(dstH / 2)

    val centerSrcW = (srcW - b.left - b.right).coerceAtLeast(0)
    val centerSrcH = (srcH - b.top - b.bottom).coerceAtLeast(0)
    val centerDstW = (dstW - leftW - rightW).coerceAtLeast(0)
    val centerDstH = (dstH - topH - bottomH).coerceAtLeast(0)

    blit(sprite, 0, 0, b.left, b.top, 0, 0, leftW, topH)
    blit(sprite, srcW - b.right, 0, b.right, b.top, dstW - rightW, 0, rightW, topH)
    blit(sprite, 0, srcH - b.bottom, b.left, b.bottom, 0, dstH - bottomH, leftW, bottomH)
    blit(sprite, srcW - b.right, srcH - b.bottom, b.right, b.bottom, dstW - rightW, dstH - bottomH, rightW, bottomH)

    if (centerDstW > 0) {
        blitInner(sprite, b.left, 0, centerSrcW, b.top, leftW, 0, centerDstW, topH, pixelScale)
        blitInner(sprite, b.left, srcH - b.bottom, centerSrcW, b.bottom, leftW, dstH - bottomH, centerDstW, bottomH, pixelScale)
    }
    if (centerDstH > 0) {
        blitInner(sprite, 0, b.top, b.left, centerSrcH, 0, topH, leftW, centerDstH, pixelScale)
        blitInner(sprite, srcW - b.right, b.top, b.right, centerSrcH, dstW - rightW, topH, rightW, centerDstH, pixelScale)
    }

    if (centerDstW > 0 && centerDstH > 0 && centerSrcW > 0 && centerSrcH > 0) {
        blitInner(sprite, b.left, b.top, centerSrcW, centerSrcH, leftW, topH, centerDstW, centerDstH, pixelScale)
    }
}

private fun DrawScope.blitInner(
    sprite: NineSliceSprite,
    srcX: Int, srcY: Int, srcW: Int, srcH: Int,
    dstX: Int, dstY: Int, dstW: Int, dstH: Int,
    pixelScale: Int
) {
    if (srcW <= 0 || srcH <= 0 || dstW <= 0 || dstH <= 0) return
    if (sprite.stretchInner) {
        blit(sprite, srcX, srcY, srcW, srcH, dstX, dstY, dstW, dstH)
        return
    }
    val tileWpx = (srcW * pixelScale).dp.roundToPx().coerceAtLeast(1)
    val tileHpx = (srcH * pixelScale).dp.roundToPx().coerceAtLeast(1)
    var x = 0
    while (x < dstW) {
        val drawWpx = minOf(tileWpx, dstW - x)
        val drawSrcW = ((drawWpx.toLong() * srcW) / tileWpx).toInt().coerceAtLeast(1)
        var y = 0
        while (y < dstH) {
            val drawHpx = minOf(tileHpx, dstH - y)
            val drawSrcH = ((drawHpx.toLong() * srcH) / tileHpx).toInt().coerceAtLeast(1)
            blit(sprite, srcX, srcY, drawSrcW, drawSrcH, dstX + x, dstY + y, drawWpx, drawHpx)
            y += drawHpx
        }
        x += drawWpx
    }
}

private fun DrawScope.blit(
    sprite: NineSliceSprite,
    srcX: Int, srcY: Int, srcW: Int, srcH: Int,
    dstX: Int, dstY: Int, dstW: Int, dstH: Int
) {
    if (srcW <= 0 || srcH <= 0 || dstW <= 0 || dstH <= 0) return
    drawImage(
        image = sprite.bitmap,
        srcOffset = IntOffset(srcX, srcY),
        srcSize = IntSize(srcW, srcH),
        dstOffset = IntOffset(dstX, dstY),
        dstSize = IntSize(dstW, dstH),
        filterQuality = FilterQuality.None
    )
}

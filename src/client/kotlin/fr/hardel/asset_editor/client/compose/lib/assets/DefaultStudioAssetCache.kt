package fr.hardel.asset_editor.client.compose.lib.assets

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import fr.hardel.asset_editor.client.compose.StudioResourceLoader
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import net.minecraft.resources.Identifier
import org.jetbrains.skia.Data
import org.jetbrains.skia.Image as SkiaImage
import org.jetbrains.skia.svg.SVGDOM
import org.jetbrains.skia.svg.SVGLength
import org.jetbrains.skia.svg.SVGLengthUnit
import org.jetbrains.skia.svg.SVGPreserveAspectRatio
import org.jetbrains.skia.svg.SVGPreserveAspectRatioAlign
import org.jetbrains.skia.svg.SVGPreserveAspectRatioScale

private const val BITMAP_CACHE_MAX = 512
private const val SVG_CACHE_MAX = 512
private const val MISSING_CACHE_MAX = 1024

class DefaultStudioAssetCache : StudioAssetCache {

    private val bitmapCache: MutableMap<String, ImageBitmap> = Collections.synchronizedMap(
        LruMap<String, ImageBitmap>(BITMAP_CACHE_MAX, onEvict = null)
    )
    private val svgCache: MutableMap<String, SvgAsset> = Collections.synchronizedMap(
        LruMap<String, SvgAsset>(SVG_CACHE_MAX, onEvict = SvgAsset::close)
    )
    private val missingBitmap = Collections.synchronizedSet(
        Collections.newSetFromMap(LruMap<String, Boolean>(MISSING_CACHE_MAX, onEvict = null))
    )
    private val missingSvg = Collections.synchronizedSet(
        Collections.newSetFromMap(LruMap<String, Boolean>(MISSING_CACHE_MAX, onEvict = null))
    )

    override fun bitmap(location: Identifier): ImageBitmap? {
        val key = location.toString()
        if (missingBitmap.contains(key)) {
            return null
        }
        bitmapCache[key]?.let { return it }

        return try {
            val bitmap = StudioResourceLoader.open(location).use { stream ->
                SkiaImage.makeFromEncoded(stream.readBytes()).toComposeImageBitmap()
            }
            bitmapCache[key] = bitmap
            bitmap
        } catch (_: Exception) {
            missingBitmap += key
            null
        }
    }

    override fun svg(location: Identifier): SvgAsset? {
        val key = location.toString()
        if (missingSvg.contains(key)) {
            return null
        }
        svgCache[key]?.let { return it }

        return try {
            val asset = StudioResourceLoader.open(location).use { stream ->
                SvgAsset(
                    SVGDOM(Data.makeFromBytes(stream.readBytes())).apply {
                        root?.apply {
                            x = SVGLength(0f)
                            y = SVGLength(0f)
                            width = SVGLength(100f, SVGLengthUnit.PERCENTAGE)
                            height = SVGLength(100f, SVGLengthUnit.PERCENTAGE)
                            preserveAspectRatio = SVGPreserveAspectRatio(
                                SVGPreserveAspectRatioAlign.XMID_YMID,
                                SVGPreserveAspectRatioScale.MEET
                            )
                        }
                    }
                )
            }
            svgCache[key] = asset
            asset
        } catch (_: Exception) {
            missingSvg += key
            null
        }
    }

    override fun invalidateAll() {
        synchronized(svgCache) { svgCache.values.forEach(SvgAsset::close) }
        bitmapCache.clear()
        svgCache.clear()
        missingBitmap.clear()
        missingSvg.clear()
    }
}

private class LruMap<K, V>(
    private val maxSize: Int,
    private val onEvict: ((V) -> Unit)?
) : java.util.LinkedHashMap<K, V>(maxSize, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>): Boolean {
        val evict = size > maxSize
        if (evict) onEvict?.invoke(eldest.value)
        return evict
    }
}

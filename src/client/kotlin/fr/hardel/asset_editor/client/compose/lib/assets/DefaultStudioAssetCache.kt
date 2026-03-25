package fr.hardel.asset_editor.client.compose.lib.assets

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import fr.hardel.asset_editor.client.compose.StudioResourceLoader
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

class DefaultStudioAssetCache : StudioAssetCache {

    private val bitmapCache = ConcurrentHashMap<String, ImageBitmap>()
    private val svgCache = ConcurrentHashMap<String, SvgAsset>()
    private val missingBitmap = ConcurrentHashMap.newKeySet<String>()
    private val missingSvg = ConcurrentHashMap.newKeySet<String>()

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
        svgCache.values.forEach(SvgAsset::close)
        bitmapCache.clear()
        svgCache.clear()
        missingBitmap.clear()
        missingSvg.clear()
    }
}

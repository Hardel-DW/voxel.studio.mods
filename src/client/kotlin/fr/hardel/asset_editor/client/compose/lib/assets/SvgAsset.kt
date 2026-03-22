package fr.hardel.asset_editor.client.compose.lib.assets

import org.jetbrains.skia.svg.SVGDOM

class SvgAsset(
    val document: SVGDOM
) {
    fun close() {
        document.close()
    }
}

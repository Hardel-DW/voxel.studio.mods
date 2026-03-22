package fr.hardel.asset_editor.client.compose.lib.assets

import androidx.compose.ui.graphics.ImageBitmap
import net.minecraft.resources.Identifier

interface StudioAssetCache {
    fun bitmap(location: Identifier): ImageBitmap?

    fun svg(location: Identifier): SvgAsset?

    fun invalidateAll()
}

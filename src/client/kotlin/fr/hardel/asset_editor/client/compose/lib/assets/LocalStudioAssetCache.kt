package fr.hardel.asset_editor.client.compose.lib.assets

import androidx.compose.runtime.compositionLocalOf

private val fallbackAssetCache = DefaultStudioAssetCache()

val LocalStudioAssetCache = compositionLocalOf<StudioAssetCache> { fallbackAssetCache }

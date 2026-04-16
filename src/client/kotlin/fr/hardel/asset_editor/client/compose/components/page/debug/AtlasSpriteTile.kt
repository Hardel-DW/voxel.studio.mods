package fr.hardel.asset_editor.client.compose.components.page.debug

import fr.hardel.asset_editor.client.rendering.NativeAtlasSnapshotService
import net.minecraft.resources.Identifier

data class AtlasSpriteTile(
    val id: Identifier,
    val sourceX: Int,
    val sourceY: Int,
    val sourceWidth: Int,
    val sourceHeight: Int
)

fun NativeAtlasSnapshotService.SpriteRegion.toAtlasSpriteTile(): AtlasSpriteTile = AtlasSpriteTile(
    id = spriteId(),
    sourceX = sourceX(),
    sourceY = sourceY(),
    sourceWidth = sourceWidth(),
    sourceHeight = sourceHeight()
)

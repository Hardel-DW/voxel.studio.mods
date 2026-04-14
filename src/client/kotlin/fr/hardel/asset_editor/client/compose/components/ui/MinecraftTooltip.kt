package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.lib.assets.drawNineSlice
import fr.hardel.asset_editor.client.compose.lib.assets.rememberNineSliceSprite
import net.minecraft.resources.Identifier

private const val MINECRAFT_PIXEL_SCALE = 3
private const val TOOLTIP_PADDING = 3
private const val TOOLTIP_MARGIN = 9
private const val TOOLTIP_OUTER = (TOOLTIP_PADDING + TOOLTIP_MARGIN) * MINECRAFT_PIXEL_SCALE

private val DEFAULT_BACKGROUND = Identifier.fromNamespaceAndPath("minecraft", "tooltip/background")
private val DEFAULT_FRAME = Identifier.fromNamespaceAndPath("minecraft", "tooltip/frame")

@Composable
fun MinecraftTooltip(
    name: String,
    modifier: Modifier = Modifier,
    enchantments: List<String> = emptyList(),
    lores: List<String> = emptyList(),
    attributes: List<String> = emptyList(),
    tooltipStyle: Identifier? = null
) {
    val background = rememberNineSliceSprite(backgroundSpriteId(tooltipStyle))
    val frame = rememberNineSliceSprite(frameSpriteId(tooltipStyle))

    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .drawBehind {
                val size = IntSize(size.width.toInt(), size.height.toInt())
                background?.let { drawNineSlice(it, size, MINECRAFT_PIXEL_SCALE) }
                frame?.let { drawNineSlice(it, size, MINECRAFT_PIXEL_SCALE) }
            }
            .padding(horizontal = TOOLTIP_OUTER.dp, vertical = TOOLTIP_OUTER.dp)
    ) {
        Text(
            text = name,
            style = StudioTypography.seven(16),
            color = StudioColors.TooltipName
        )
        for (lore in lores) {
            Text(
                text = lore,
                style = StudioTypography.seven(16),
                color = StudioColors.TooltipLore
            )
        }
        for (enchantment in enchantments) {
            Text(
                text = enchantment,
                style = StudioTypography.seven(16),
                color = StudioColors.TooltipEnchant
            )
        }
        for (attribute in attributes) {
            Text(
                text = attribute,
                style = StudioTypography.seven(16),
                color = StudioColors.TooltipAttribute
            )
        }
    }
}

private fun backgroundSpriteId(style: Identifier?): Identifier =
    style?.withPath { "tooltip/${it}_background" } ?: DEFAULT_BACKGROUND

private fun frameSpriteId(style: Identifier?): Identifier =
    style?.withPath { "tooltip/${it}_frame" } ?: DEFAULT_FRAME

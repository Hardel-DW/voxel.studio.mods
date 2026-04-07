package fr.hardel.asset_editor.client.compose.components.layout.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.ResourceImageIcon
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.components.ui.tree.ConceptTreeState
import fr.hardel.asset_editor.client.compose.components.ui.tree.TreeSidebarView
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.utils.BrowserUtils
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val DISCORD_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/company/discord.svg")
private val DISCORD_CARD_SHAPE = RoundedCornerShape(8.dp)
private val borderColor = StudioColors.Zinc800.copy(alpha = 0.5f)

@Composable
fun EditorSidebar(
    context: StudioContext,
    treeState: ConceptTreeState,
    titleKey: String,
    iconPath: Identifier,
    topContent: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier
) {
    // aside: w-72 shrink-0 border-r border-zinc-800/50 bg-zinc-950/75 flex flex-col z-20
    Column(
        modifier = modifier
            .width(288.dp)
            .fillMaxHeight()
            .background(StudioColors.SecondarySidebar)
            .drawWithContent {
                drawContent()
                val stroke = 1.dp.toPx()
                drawLine(
                    borderColor,
                    Offset(size.width - stroke / 2f, 0f),
                    Offset(size.width - stroke / 2f, size.height),
                    strokeWidth = stroke
                )
            }
    ) {
        // div: px-6 pt-6
        Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp)) {
            // Link: text-lg font-bold text-zinc-100 flex items-center gap-2 mb-1
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(onClick = treeState.onSelectAll)
            ) {
                // img: size-5 opacity-80
                ResourceImageIcon(iconPath, 20.dp, modifier = Modifier.alpha(0.8f))
                Text(
                    text = I18n.get(titleKey),
                    style = StudioTypography.bold(18),
                    color = StudioColors.Zinc100
                )
            }
            // p: text-xs text-zinc-500 pl-7
            Text(
                text = I18n.get("generic:explore"),
                style = StudioTypography.regular(12),
                color = StudioColors.Zinc500,
                modifier = Modifier.padding(start = 28.dp)
            )
        }

        // div: flex-1 overflow-y-auto px-3
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            topContent.forEach { it() }
            TreeSidebarView(treeState = treeState, modifier = Modifier.weight(1f))
        }

        // div: p-4 border-t border-zinc-800/50 bg-zinc-950/90
        DiscordFooter()
    }
}

@Composable
private fun DiscordFooter() {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    // div: p-4 border-t border-zinc-800/50 bg-zinc-950/90
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(StudioColors.Zinc950.copy(alpha = 0.9f))
            .drawWithContent {
                val stroke = 1.dp.toPx()
                drawLine(
                    borderColor,
                    Offset(0f, stroke / 2f),
                    Offset(size.width, stroke / 2f),
                    strokeWidth = stroke
                )
                drawContent()
            }
            .padding(16.dp)
    ) {
        // a: bg-zinc-900/30 rounded-lg p-3 border border-zinc-800/50 flex items-center gap-3
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioColors.Zinc900.copy(alpha = 0.3f), DISCORD_CARD_SHAPE)
                .border(
                    1.dp,
                    if (hovered) StudioColors.Zinc700.copy(alpha = 0.5f)
                    else StudioColors.Zinc800.copy(alpha = 0.5f),
                    DISCORD_CARD_SHAPE
                )
                .hoverable(interaction)
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable(
                    interactionSource = interaction,
                    indication = null
                ) { BrowserUtils.openBrowser("https://discord.gg/TAmVFvkHep") }
                .padding(12.dp)
        ) {
            // div.flex-1 > div: text-sm font-medium text-zinc-300 group-hover:text-white
            Text(
                text = I18n.get("supports:help.discord"),
                style = StudioTypography.medium(14),
                color = if (hovered) Color.White else StudioColors.Zinc300,
                modifier = Modifier.weight(1f)
            )

            // div: size-8 rounded-full bg-zinc-800/50 group-hover:bg-zinc-800
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        if (hovered) StudioColors.Zinc800 else StudioColors.Zinc800.copy(alpha = 0.5f),
                        CircleShape
                    )
            ) {
                // img: size-4 invert opacity-30 group-hover:opacity-50
                SvgIcon(DISCORD_ICON, 16.dp, Color.White, modifier = Modifier.alpha(if (hovered) 0.5f else 0.3f))
            }
        }
    }
}

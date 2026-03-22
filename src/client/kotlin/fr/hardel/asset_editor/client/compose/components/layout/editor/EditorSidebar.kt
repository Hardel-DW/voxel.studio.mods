package fr.hardel.asset_editor.client.compose.components.layout.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import fr.hardel.asset_editor.client.compose.components.ui.ResourceImageIcon
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.components.ui.tree.TreeController
import fr.hardel.asset_editor.client.compose.components.ui.tree.TreeSidebarView
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.utils.BrowserUtils
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val DISCORD_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/company/discord.svg")

@Composable
fun EditorSidebar(
    context: StudioContext,
    tree: TreeController,
    titleKey: String,
    iconPath: Identifier,
    topContent: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(288.dp)
            .fillMaxHeight()
            .background(VoxelColors.SidebarBg)
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawLine(
                    color = VoxelColors.BorderAlpha50,
                    start = Offset(size.width - stroke / 2f, 0f),
                    end = Offset(size.width - stroke / 2f, size.height),
                    strokeWidth = stroke
                )
            }
    ) {
        Column(
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(onClick = tree::selectAll)
            ) {
                ResourceImageIcon(iconPath, 20.dp)
                Text(
                    text = I18n.get(titleKey),
                    style = VoxelTypography.bold(18),
                    color = VoxelColors.Zinc100
                )
            }
            Text(
                text = I18n.get("generic:explore"),
                style = VoxelTypography.regular(12),
                color = VoxelColors.Zinc500,
                modifier = Modifier.padding(start = 28.dp, top = 2.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
        ) {
            topContent.forEach { extra ->
                extra()
            }
            TreeSidebarView(tree = tree)
        }

        val interaction = remember { MutableInteractionSource() }
        val hovered by interaction.collectIsHoveredAsState()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(VoxelColors.Sidebar)
                .drawBehind {
                    val stroke = 1.dp.toPx()
                    drawLine(
                        color = VoxelColors.BorderAlpha50,
                        start = Offset(0f, stroke / 2f),
                        end = Offset(size.width, stroke / 2f),
                        strokeWidth = stroke
                    )
                }
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VoxelColors.DiscordCardBg, RoundedCornerShape(8.dp))
                    .border(1.dp, if (hovered) VoxelColors.DiscordCardBorderHover else VoxelColors.DiscordCardBorder, RoundedCornerShape(8.dp))
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(
                        interactionSource = interaction,
                        indication = null
                    ) { BrowserUtils.openBrowser("https://discord.gg/TAmVFvkHep") }
                    .padding(12.dp)
            ) {
                Text(
                    text = I18n.get("supports:help.discord"),
                    style = VoxelTypography.medium(14),
                    color = VoxelColors.Zinc300,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = if (hovered) VoxelColors.DiscordCircleBgHover else VoxelColors.DiscordCircleBg,
                            shape = CircleShape
                        )
                ) {
                    SvgIcon(DISCORD_ICON, 16.dp, Color.White, modifier = Modifier.alpha(if (hovered) 0.5f else 0.3f))
                }
            }
        }
    }
}

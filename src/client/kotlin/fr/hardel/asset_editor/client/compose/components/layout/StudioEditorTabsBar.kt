package fr.hardel.asset_editor.client.compose.components.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.ResourceImageIcon
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.StudioTranslation
import fr.hardel.asset_editor.client.compose.lib.rememberActiveTabId
import fr.hardel.asset_editor.client.compose.lib.rememberOpenTabs
import fr.hardel.asset_editor.client.compose.lib.StudioTabEntry
import net.minecraft.resources.Identifier

private val CLOSE_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/close.svg")

@Composable
fun StudioEditorTabsBar(context: StudioContext, modifier: Modifier = Modifier) {
    val openTabs = rememberOpenTabs(context)
    val activeTabId = rememberActiveTabId(context)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(start = 8.dp)
    ) {
        PackSelector(context = context)
        Box(
            modifier = Modifier
                .size(width = 1.dp, height = 24.dp)
                .background(StudioColors.Zinc700)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            openTabs.forEachIndexed { index, tab ->
                key(tab.tabId) {
                    StudioEditorTabItem(
                        context = context,
                        tab = tab,
                        active = tab.tabId == activeTabId
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun StudioEditorTabItem(
    context: StudioContext,
    tab: StudioTabEntry,
    active: Boolean
) {
    val itemInteraction = remember { MutableInteractionSource() }
    val itemHovered by itemInteraction.collectIsHoveredAsState()
    val closeInteraction = remember { MutableInteractionSource() }
    val closeHovered by closeInteraction.collectIsHoveredAsState()
    val conceptId = tab.destination.conceptId
    val conceptRegistryKey = context.studioRegistryKey(conceptId)
    val identifier = Identifier.tryParse(tab.destination.elementId)
    val label = if (identifier != null) {
        StudioTranslation.resolve(conceptRegistryKey, identifier)
    } else {
        tab.destination.elementId
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .background(
                color = when {
                    active -> StudioColors.Zinc800.copy(alpha = 0.8f)
                    itemHovered -> StudioColors.Zinc800.copy(alpha = 0.5f)
                    else -> Color.Transparent
                },
                shape = RoundedCornerShape(6.dp)
            )
            .hoverable(itemInteraction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = itemInteraction,
                indication = null
            ) {
                context.navigationMemory().switchTab(tab.tabId)
            }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        ResourceImageIcon(
            location = context.studioIcon(conceptId),
            size = 16.dp,
            modifier = Modifier.alpha(if (active) 1f else 0.85f)
        )
        Text(
            text = label,
            style = StudioTypography.medium(14),
            color = if (active) StudioColors.Zinc100 else StudioColors.Zinc400,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier.widthIn(max = 192.dp)
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(16.dp)
                .alpha(if (active || itemHovered) 1f else 0f)
                .background(
                    color = if (closeHovered) StudioColors.Zinc700 else Color.Transparent,
                    shape = RoundedCornerShape(4.dp)
                )
                .hoverable(closeInteraction)
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable(
                    interactionSource = closeInteraction,
                    indication = null
                ) {
                    context.navigationMemory().closeTab(tab.tabId)
                }
        ) {
            SvgIcon(
                location = CLOSE_ICON,
                size = 10.dp,
                tint = if (closeHovered) Color.White else if (active) StudioColors.Zinc200 else StudioColors.Zinc400
            )
        }
    }
}

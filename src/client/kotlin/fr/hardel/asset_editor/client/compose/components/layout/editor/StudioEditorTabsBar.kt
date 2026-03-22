package fr.hardel.asset_editor.client.compose.components.layout.editor

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
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import fr.hardel.asset_editor.client.compose.components.layout.loading.WindowControls
import fr.hardel.asset_editor.client.compose.components.ui.ResourceImageIcon
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.StudioText
import fr.hardel.asset_editor.client.compose.lib.data.StudioConcept
import fr.hardel.asset_editor.client.compose.lib.data.StudioElementId
import fr.hardel.asset_editor.client.compose.routes.StudioRoute
import fr.hardel.asset_editor.client.compose.window.RememberWindowDragArea
import fr.hardel.asset_editor.client.compose.window.windowDragArea
import net.minecraft.resources.Identifier

private val CLOSE_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/close.svg")
private const val EDITOR_TABS_DRAG_AREA = "editor_tabs_drag_area"

@Composable
fun StudioEditorTabsBar(context: StudioContext, modifier: Modifier = Modifier) {
    RememberWindowDragArea(EDITOR_TABS_DRAG_AREA)

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
                .background(VoxelColors.BorderHover)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            context.openTabs.forEachIndexed { index, tab ->
                StudioEditorTabItem(
                    context = context,
                    tab = tab,
                    index = index,
                    active = index == context.activeTabIndex
                )
            }
        }

        Spacer(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .windowDragArea(EDITOR_TABS_DRAG_AREA)
        )

        WindowControls(buttonWidth = 48.dp, buttonHeight = 48.dp)
    }
}

@Composable
private fun StudioEditorTabItem(
    context: StudioContext,
    tab: StudioContext.OpenTab,
    index: Int,
    active: Boolean
) {
    val itemInteraction = remember(index) { MutableInteractionSource() }
    val itemHovered by itemInteraction.collectIsHoveredAsState()
    val closeInteraction = remember(index) { MutableInteractionSource() }
    val closeHovered by closeInteraction.collectIsHoveredAsState()
    val concept = StudioConcept.byRoute(tab.route)
    val parsed = StudioElementId.parse(tab.elementId)
    val label = if (parsed != null) {
        StudioText.resolve(concept.registryKey, parsed.identifier)
    } else {
        tab.elementId
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .background(
                color = when {
                    active -> VoxelColors.ActiveBg
                    itemHovered -> VoxelColors.Zinc900
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
                context.tabsState().switchTab(index)
                context.router.navigate(tab.route)
            }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        ResourceImageIcon(
            location = concept.icon,
            size = 16.dp,
            modifier = Modifier.alpha(if (active) 1f else 0.85f)
        )
        Text(
            text = label,
            style = VoxelTypography.medium(14),
            color = if (active) VoxelColors.Zinc100 else VoxelColors.Zinc400,
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
                    color = if (closeHovered) VoxelColors.BorderHover else Color.Transparent,
                    shape = RoundedCornerShape(4.dp)
                )
                .hoverable(closeInteraction)
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable(
                    interactionSource = closeInteraction,
                    indication = null
                ) {
                    context.tabsState().closeTab(index)
                    val next = context.tabsState().activeTab()
                    val fallback = StudioRoute.overviewOf(context.router.currentRoute.concept())
                    context.router.navigate(next?.route() ?: fallback)
                }
        ) {
            SvgIcon(
                location = CLOSE_ICON,
                size = 10.dp,
                tint = if (closeHovered) Color.White else if (active) VoxelColors.Zinc200 else VoxelColors.Zinc400
            )
        }
    }
}

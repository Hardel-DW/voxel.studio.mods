package fr.hardel.asset_editor.client.compose.components.layout.loading

import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.window.VoxelStudioWindow
import net.minecraft.resources.Identifier

private val MINIMIZE_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/window/minimize.svg")
private val MAXIMIZE_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/window/maximize.svg")
private val CLOSE_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/window/close.svg")

@Composable
fun WindowControls(
    buttonWidth: Dp = 36.dp,
    buttonHeight: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.Start) {
        WindowButton(MINIMIZE_ICON, StudioColors.Zinc200, buttonWidth, buttonHeight) { VoxelStudioWindow.requestMinimize() }
        WindowButton(MAXIMIZE_ICON, StudioColors.Zinc200, buttonWidth, buttonHeight) { VoxelStudioWindow.requestToggleMaximize() }
        WindowButton(CLOSE_ICON, StudioColors.Red400, buttonWidth, buttonHeight) { VoxelStudioWindow.requestClose() }
    }
}

@Composable
private fun WindowButton(
    icon: Identifier,
    hoverTint: androidx.compose.ui.graphics.Color,
    width: Dp,
    height: Dp,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isHovered by interaction.collectIsHoveredAsState()

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width, height)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        SvgIcon(
            location = icon,
            size = 12.dp,
            tint = if (isHovered) hoverTint else StudioColors.Zinc500
        )
    }
}

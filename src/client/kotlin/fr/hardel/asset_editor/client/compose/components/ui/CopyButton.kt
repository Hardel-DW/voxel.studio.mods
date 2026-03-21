package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.VoxelColors
import kotlinx.coroutines.delay
import net.minecraft.resources.Identifier
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

private val COPY_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/copy.svg")
private val CHECK_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/check.svg")

@Composable
fun CopyButton(
    textProvider: () -> String,
    modifier: Modifier = Modifier
) {
    var showCheck by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    if (showCheck) {
        LaunchedEffect(Unit) {
            delay(1200)
            showCheck = false
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(24.dp)
            .pointerHoverIcon(PointerIcon.Hand)
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                clipboard.setContents(StringSelection(textProvider()), null)
                showCheck = true
            }
    ) {
        if (showCheck) {
            SvgIcon(CHECK_ICON, 14.dp, VoxelColors.Emerald400)
        } else {
            SvgIcon(COPY_ICON, 14.dp, if (isHovered) VoxelColors.Zinc300 else VoxelColors.Zinc500)
        }
    }
}

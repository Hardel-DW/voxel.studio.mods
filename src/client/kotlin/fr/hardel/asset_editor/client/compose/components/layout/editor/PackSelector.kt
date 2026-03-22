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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import fr.hardel.asset_editor.client.compose.components.ui.ShineOverlay
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.state.ClientPackInfo
import kotlin.math.roundToInt
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val FOLDER_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/folder.svg")
private val CHEVRON_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/chevron-down.svg")
private val PENCIL_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/pencil.svg")
private val packSelectorShape = RoundedCornerShape(8.dp)

@Composable
fun PackSelector(
    context: StudioContext,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var anchorPosition by remember { mutableStateOf(IntOffset.Zero) }
    var anchorSize by remember { mutableStateOf(IntSize.Zero) }

    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val selectedPack = context.selectedPack

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .width(280.dp)
            .height(36.dp)
            .onGloballyPositioned { coordinates ->
                val position = coordinates.positionInWindow()
                anchorPosition = IntOffset(position.x.roundToInt(), position.y.roundToInt())
                anchorSize = coordinates.size
            }
            .background(if (hovered) Color(0xFF18181B) else Color.Transparent, packSelectorShape)
            .border(1.dp, if (hovered) Color(0xFF3F3F46) else Color(0xFF27272A), packSelectorShape)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interaction,
                indication = null
            ) { expanded = !expanded }
            .padding(horizontal = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SvgIcon(FOLDER_ICON, 14.dp, VoxelColors.Zinc400)
            Text(
                text = selectedPack?.name() ?: I18n.get("studio:pack.none"),
                style = VoxelTypography.medium(12),
                color = if (selectedPack == null) VoxelColors.Zinc500 else VoxelColors.Zinc200,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            SvgIcon(CHEVRON_ICON, 10.dp, VoxelColors.Zinc500)
        }
    }

    if (expanded) {
        Popup(
            offset = IntOffset(anchorPosition.x, anchorPosition.y + anchorSize.height + 8),
            onDismissRequest = { expanded = false },
            properties = PopupProperties(focusable = true)
        ) {
            Box(
                modifier = Modifier
                    .width(280.dp)
                    .background(Color(0xFF09090B), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF27272A), RoundedCornerShape(16.dp))
            ) {
                ShineOverlay(opacity = 0.12f)

                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = I18n.get("studio:pack.select"),
                        style = VoxelTypography.semiBold(13),
                        color = VoxelColors.Zinc300,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    context.availablePacks.forEach { pack ->
                        PackRow(
                            pack = pack,
                            selected = pack == selectedPack,
                            onClick = {
                                context.packState().selectPack(pack)
                                expanded = false
                            }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .padding(vertical = 6.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFF27272A))
                    )

                    CreatePackButton {
                        expanded = false
                        showCreateDialog = true
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        PackCreateDialog.create(
            context = context,
            onDismiss = { showCreateDialog = false }
        )
    }
}

@Composable
private fun PackRow(
    pack: ClientPackInfo,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (!selected && hovered) Color(0xFF18181B) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        SvgIcon(
            location = FOLDER_ICON,
            size = 14.dp,
            tint = if (selected) VoxelColors.Zinc200 else VoxelColors.Zinc400
        )
        Text(
            text = pack.name(),
            style = if (selected) VoxelTypography.semiBold(13) else VoxelTypography.medium(13),
            color = if (selected) VoxelColors.Zinc100 else VoxelColors.Zinc300
        )
    }
}

@Composable
private fun CreatePackButton(
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (hovered) Color(0xFF18181B) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        SvgIcon(PENCIL_ICON, 12.dp, VoxelColors.Zinc400)
        Text(
            text = I18n.get("studio:pack.create"),
            style = VoxelTypography.medium(12),
            color = VoxelColors.Zinc300
        )
    }
}

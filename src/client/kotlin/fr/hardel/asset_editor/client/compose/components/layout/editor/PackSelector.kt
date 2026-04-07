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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
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
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.ShineOverlay
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.rememberAvailablePacks
import fr.hardel.asset_editor.client.compose.lib.rememberSelectedPack
import fr.hardel.asset_editor.client.memory.session.ui.ClientPackInfo
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
    val selectedPack = rememberSelectedPack(context)
    val availablePacks = rememberAvailablePacks(context)

    // Compose-only: sélecteur de pack dans la barre supérieure, pas d'équivalent TSX direct.
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
            .background(if (hovered) StudioColors.Zinc900 else Color.Transparent, packSelectorShape)
            .border(1.dp, if (hovered) StudioColors.Zinc700 else StudioColors.Zinc800, packSelectorShape)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interaction,
                indication = null
            ) { expanded = !expanded }
            .padding(horizontal = 10.dp)
    ) {
        // div: flex items-center gap-2
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SvgIcon(FOLDER_ICON, 14.dp, StudioColors.Zinc400)
            Text(
                text = selectedPack?.name() ?: I18n.get("studio:pack.none"),
                style = StudioTypography.medium(12),
                color = if (selectedPack == null) StudioColors.Zinc500 else StudioColors.Zinc200,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            SvgIcon(CHEVRON_ICON, 10.dp, StudioColors.Zinc500)
        }
    }

    if (expanded) {
        Popup(
            offset = IntOffset(anchorPosition.x, anchorPosition.y + anchorSize.height + 8),
            onDismissRequest = { expanded = false },
            properties = PopupProperties(focusable = true)
        ) {
            val popupShape = RoundedCornerShape(16.dp)

            // popover/content: rounded-2xl border border-zinc-800 bg-zinc-950 p-4 shadow-md
            Box(
                modifier = Modifier
                    .width(280.dp)
                    .shadow(20.dp, popupShape, ambientColor = Color.Black.copy(alpha = 0.6f), spotColor = Color.Black.copy(alpha = 0.6f))
                    .border(1.dp, StudioColors.Zinc800, popupShape)
                    .background(StudioColors.Zinc950, popupShape)
                    .clip(popupShape)
            ) {
                ShineOverlay(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            scaleY = 0.35f
                            transformOrigin = TransformOrigin(0.5f, 0f)
                        },
                    opacity = 0.12f
                )

                // div: flex flex-col gap-? p-4
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = I18n.get("studio:pack.select"),
                        style = StudioTypography.semiBold(13),
                        color = StudioColors.Zinc300,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    availablePacks.forEach { pack ->
                        PackRow(
                            pack = pack,
                            selected = pack == selectedPack,
                            onClick = {
                                context.packSelectionMemory().selectPack(pack)
                                expanded = false
                            }
                        )
                    }

                    // div: separator
                    Box(
                        modifier = Modifier
                            .padding(vertical = 6.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(StudioColors.Zinc800)
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

    // button/row: compose-only pack item in popup list
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (!selected && hovered) StudioColors.Zinc900 else Color.Transparent,
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
            tint = if (selected) StudioColors.Zinc200 else StudioColors.Zinc400
        )
        Text(
            text = pack.name(),
            style = if (selected) StudioTypography.semiBold(13) else StudioTypography.medium(13),
            color = if (selected) StudioColors.Zinc100 else StudioColors.Zinc300
        )
    }
}

@Composable
private fun CreatePackButton(
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    // button/row: compose-only "create pack" action in popup
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (hovered) StudioColors.Zinc900 else Color.Transparent,
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
        SvgIcon(PENCIL_ICON, 12.dp, StudioColors.Zinc400)
        Text(
            text = I18n.get("studio:pack.create"),
            style = StudioTypography.medium(12),
            color = StudioColors.Zinc300
        )
    }
}

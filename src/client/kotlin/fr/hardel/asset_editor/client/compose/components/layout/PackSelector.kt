package fr.hardel.asset_editor.client.compose.components.layout

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.PopupEnterAnimation
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.ShineOverlay
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.rememberAvailablePacks
import fr.hardel.asset_editor.client.compose.lib.rememberSelectedPack
import fr.hardel.asset_editor.client.memory.session.ui.ClientPackInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

private val LOGO_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/logo.svg")
private val CHEVRON_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/chevron-down.svg")
private val PLUS_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/plus.svg")
private val CHECK_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/check.svg")
private val LOCK_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/lock.svg")
private val triggerShape = RoundedCornerShape(10.dp)
private val popupShape = RoundedCornerShape(14.dp)
private val itemShape = RoundedCornerShape(8.dp)
private val iconShape = RoundedCornerShape(8.dp)
private val POPUP_GAP = 8.dp


/** Aligns the popup left edge with the anchor and stacks it [gap] pixels below it. */
private class BelowAnchorPositionProvider(private val gap: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ) = IntOffset(anchorBounds.left, anchorBounds.bottom + gap)
}

@Composable
fun PackSelector(
    context: StudioContext,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val selectedPack = rememberSelectedPack(context)
    val availablePacks = rememberAvailablePacks(context)

    val triggerBg by animateColorAsState(
        targetValue = when {
            expanded -> StudioColors.Zinc900
            hovered -> StudioColors.Zinc900.copy(alpha = 0.7f)
            else -> Color.Transparent
        },
        animationSpec = StudioMotion.hoverSpec(),
        label = "pack-trigger-bg"
    )
    val triggerBorder by animateColorAsState(
        targetValue = when {
            expanded -> StudioColors.Zinc700
            hovered -> StudioColors.Zinc700.copy(alpha = 0.8f)
            else -> StudioColors.Zinc800
        },
        animationSpec = StudioMotion.hoverSpec(),
        label = "pack-trigger-border"
    )
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = StudioMotion.hoverSpec(),
        label = "pack-chevron"
    )

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .width(280.dp)
            .height(40.dp)
            .background(triggerBg, triggerShape)
            .border(1.dp, triggerBorder, triggerShape)
            .clip(triggerShape)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interaction,
                indication = null
            ) { expanded = !expanded }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)
        ) {
            if (selectedPack != null) {
                if (selectedPack.icon().isNotEmpty()) {
                    PackIconAvatar(pack = selectedPack, size = 28.dp)
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(iconShape)
                            .background(StudioColors.Zinc900)
                    ) {
                        SvgIcon(LOGO_ICON, 14.dp, StudioColors.Zinc600)
                    }
                }
                Text(
                    text = selectedPack.name(),
                    style = StudioTypography.medium(13),
                    color = StudioColors.Zinc100,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(iconShape)
                        .background(StudioColors.Zinc900)
                ) {
                    SvgIcon(LOGO_ICON, 14.dp, StudioColors.Zinc600)
                }
                Text(
                    text = I18n.get("studio:pack.none"),
                    style = StudioTypography.medium(13),
                    color = StudioColors.Zinc500,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }
            SvgIcon(
                location = CHEVRON_ICON,
                size = 10.dp,
                tint = if (expanded) StudioColors.Zinc300 else StudioColors.Zinc500,
                modifier = Modifier.rotate(chevronRotation)
            )
        }

        if (expanded) {
            PackSelectorPopup(
                availablePacks = availablePacks,
                selectedPack = selectedPack,
                onSelect = { pack ->
                    context.packSelectionMemory().selectPack(pack)
                    expanded = false
                },
                onCreate = {
                    expanded = false
                    showCreateDialog = true
                },
                onDismiss = { expanded = false }
            )
        }
    }

    if (showCreateDialog) {
        PackCreateDialog.create(onDismiss = { })
    }
}

@Composable
private fun PackSelectorPopup(
    availablePacks: List<ClientPackInfo>,
    selectedPack: ClientPackInfo?,
    onSelect: (ClientPackInfo) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit
) {
    val gapPx = with(LocalDensity.current) { POPUP_GAP.roundToPx() }
    val positionProvider = remember(gapPx) { BelowAnchorPositionProvider(gapPx) }
    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        PopupEnterAnimation(
            transformOrigin = TransformOrigin(0f, 0f),
            modifier = Modifier.width(280.dp)
        ) {
            Box(
                modifier = Modifier
                    .shadow(
                        20.dp,
                        popupShape,
                        ambientColor = Color.Black.copy(alpha = 0.5f),
                        spotColor = Color.Black.copy(alpha = 0.5f)
                    )
                    .clip(popupShape)
                    .background(StudioColors.Zinc950, popupShape)
                    .border(1.dp, StudioColors.Zinc800, popupShape)
            ) {
                ShineOverlay(
                    modifier = Modifier.matchParentSize(),
                    opacity = 0.1f,
                    coverage = 0.3f
                )

                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    PopupHeader(count = availablePacks.size)

                    Column(
                        modifier = Modifier
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        availablePacks.forEach { pack ->
                            PackRow(
                                pack = pack,
                                selected = pack == selectedPack,
                                onClick = { onSelect(pack) }
                            )
                        }
                    }

                    Separator()
                    CreatePackRow(onClick = onCreate)
                }
            }
        }
    }
}

@Composable
private fun PopupHeader(count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = I18n.get("studio:pack.select").uppercase(),
            style = StudioTypography.medium(10).copy(letterSpacing = 1.2.sp),
            color = StudioColors.Zinc500
        )
        Box(modifier = Modifier.weight(1f))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .background(StudioColors.Zinc900, RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = count.toString(),
                style = StudioTypography.semiBold(10),
                color = StudioColors.Zinc400
            )
        }
    }
    Separator()
}

@Composable
private fun Separator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .height(1.dp)
            .background(StudioColors.Zinc900)
    )
}

@Composable
private fun PackRow(
    pack: ClientPackInfo,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    val rowBg by animateColorAsState(
        targetValue = when {
            hovered -> StudioColors.Zinc800.copy(alpha = 0.75f)
            selected -> StudioColors.Zinc800.copy(alpha = 0.35f)
            else -> Color.Transparent
        },
        animationSpec = StudioMotion.hoverSpec(),
        label = "pack-row-bg"
    )
    val nameColor by animateColorAsState(
        targetValue = when {
            hovered -> StudioColors.Zinc50
            selected -> StudioColors.Zinc100
            else -> StudioColors.Zinc200
        },
        animationSpec = StudioMotion.hoverSpec(),
        label = "pack-row-name"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(itemShape)
            .background(rowBg, itemShape)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        PackIconAvatar(pack = pack, size = 28.dp)

        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = pack.name(),
                style = StudioTypography.semiBold(13),
                color = nameColor,
                maxLines = 1
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (!pack.writable()) {
                    SvgIcon(LOCK_ICON, 10.dp, StudioColors.Amber400)
                    Text(
                        text = I18n.get("studio:pack.readonly"),
                        style = StudioTypography.regular(11),
                        color = StudioColors.Amber400
                    )
                } else {
                    Text(
                        text = packSubtitle(pack),
                        style = StudioTypography.regular(11),
                        color = StudioColors.Zinc500
                    )
                }
            }
        }

        if (selected) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudioColors.Violet500.copy(alpha = 0.2f))
            ) {
                SvgIcon(CHECK_ICON, 12.dp, Color.White)
            }
        }
    }
}

@Composable
private fun CreatePackRow(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    val bg by animateColorAsState(
        targetValue = if (hovered) StudioColors.Zinc800.copy(alpha = 0.75f) else Color.Transparent,
        animationSpec = StudioMotion.hoverSpec(),
        label = "pack-create-bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (hovered) StudioColors.Zinc50 else StudioColors.Zinc200,
        animationSpec = StudioMotion.hoverSpec(),
        label = "pack-create-text"
    )
    val iconTint by animateColorAsState(
        targetValue = if (hovered) StudioColors.Zinc200 else StudioColors.Zinc400,
        animationSpec = StudioMotion.hoverSpec(),
        label = "pack-create-icon"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .clip(itemShape)
            .background(bg, itemShape)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 6.dp, vertical = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .clip(iconShape)
                .background(StudioColors.Zinc950)
                .border(1.dp, StudioColors.Zinc800, iconShape)
        ) {
            SvgIcon(PLUS_ICON, 12.dp, iconTint)
        }
        Text(
            text = I18n.get("studio:pack.create"),
            style = StudioTypography.semiBold(13),
            color = textColor
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PackIconAvatar(pack: ClientPackInfo, size: Dp) {
    val bitmap = rememberPackIcon(pack.icon())
    PackIconSurface(pack = pack, size = size, shape = iconShape) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = pack.name(),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            SvgIcon(LOGO_ICON, size * 0.5f, StudioColors.Zinc600)
        }
    }
}

@Composable
private fun PackIconSurface(pack: ClientPackInfo, size: Dp, shape: Shape, content: @Composable () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(StudioColors.Zinc900)
            .border(1.dp, StudioColors.Zinc800, shape)
    ) {
        content()
    }
}

@Composable
private fun rememberPackIcon(icon: ByteArray): ImageBitmap? {
    var bitmap by remember(icon) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(icon) {
        bitmap = if (icon.isEmpty()) null
        else withContext(Dispatchers.IO) {
            runCatching { ImageIO.read(ByteArrayInputStream(icon))?.toComposeImageBitmap() }.getOrNull()
        }
    }
    return bitmap
}


private fun packSubtitle(pack: ClientPackInfo): String {
    return when (val count = pack.namespaces().size) {
        0 -> pack.packId().removePrefix("file/")
        1 -> pack.namespaces().first()
        else -> "${pack.namespaces().first()} +${count - 1}"
    }
}

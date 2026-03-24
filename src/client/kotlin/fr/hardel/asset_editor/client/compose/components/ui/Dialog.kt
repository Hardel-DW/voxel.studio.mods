package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import net.minecraft.resources.Identifier

private val CLOSE_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/close.svg")

@Composable
fun Dialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animProgress.animateTo(1f, tween(200))
    }

    Popup(
        alignment = Alignment.Center,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = animProgress.value }
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() }
        ) {
            Box(
                modifier = modifier
                    .widthIn(min = 360.dp, max = 460.dp)
                    .graphicsLayer {
                        val p = animProgress.value
                        scaleX = 0.95f + 0.05f * p
                        scaleY = 0.95f + 0.05f * p
                        translationY = (1f - p) * 16.dp.toPx()
                        alpha = p
                    }
                    .shadow(24.dp, shape, ambientColor = Color.Black.copy(alpha = 0.6f), spotColor = Color.Black.copy(alpha = 0.6f))
                    .border(1.dp, VoxelColors.Zinc800, shape)
                    .background(VoxelColors.Zinc950, shape)
                    .clip(shape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
            ) {
                ShineOverlay(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            scaleY = 0.4f
                            transformOrigin = TransformOrigin(0.5f, 0f)
                        },
                    opacity = 0.15f
                )

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp)
                    ) {
                        Text(
                            text = title,
                            style = VoxelTypography.semiBold(18),
                            color = VoxelColors.Zinc100
                        )
                        Spacer(Modifier.weight(1f))
                        val closeInteraction = remember { MutableInteractionSource() }
                        val closeHovered by closeInteraction.collectIsHoveredAsState()
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (closeHovered) VoxelColors.Zinc800 else Color.Transparent)
                                .hoverable(closeInteraction)
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clickable(
                                    interactionSource = closeInteraction,
                                    indication = null
                                ) { onDismiss() }
                        ) {
                            SvgIcon(CLOSE_ICON, 16.dp, if (closeHovered) VoxelColors.Zinc100 else VoxelColors.Zinc400)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
                    ) {
                        content()
                    }

                    if (footer != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(Modifier.weight(1f))
                            footer()
                        }
                    }
                }
            }
        }
    }
}

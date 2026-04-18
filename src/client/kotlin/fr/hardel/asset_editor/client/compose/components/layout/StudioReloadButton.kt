package fr.hardel.asset_editor.client.compose.components.layout

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.AnimatedCheckmark
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.network.ClientPayloadSender
import fr.hardel.asset_editor.network.ReloadRequestPayload
import kotlinx.coroutines.delay
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val RELOAD_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/reload.svg")
private val CARD_SHAPE = RoundedCornerShape(8.dp)

private const val SPIN_DURATION_MS = 700
private const val SUCCESS_HOLD_MS = 1200L

private enum class ReloadState { IDLE, LOADING, SUCCESS }

@Composable
fun StudioReloadButton(modifier: Modifier = Modifier) {
    var state by remember { mutableStateOf(ReloadState.IDLE) }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val rotation = remember { Animatable(0f) }
    val checkProgress = remember { Animatable(0f) }

    LaunchedEffect(state) {
        when (state) {
            ReloadState.LOADING -> {
                checkProgress.snapTo(0f)
                rotation.snapTo(0f)
                rotation.animateTo(
                    targetValue = 360f,
                    animationSpec = tween(SPIN_DURATION_MS, easing = StudioMotion.Standard)
                )
                state = ReloadState.SUCCESS
            }
            ReloadState.SUCCESS -> {
                checkProgress.animateTo(1f, StudioMotion.checkmarkSpec())
                delay(SUCCESS_HOLD_MS)
                state = ReloadState.IDLE
            }
            ReloadState.IDLE -> {
                rotation.snapTo(0f)
                checkProgress.snapTo(0f)
            }
        }
    }

    val success = state == ReloadState.SUCCESS
    val backgroundColor = StudioColors.Zinc900.copy(alpha = 0.3f)
    val borderColor by animateColorAsState(
        targetValue = if (hovered) StudioColors.Zinc700.copy(alpha = 0.5f)
        else StudioColors.Zinc800.copy(alpha = 0.5f),
        animationSpec = StudioMotion.hoverSpec(),
        label = "reload-border"
    )
    val titleColor by animateColorAsState(
        targetValue = if (hovered) Color.White else StudioColors.Zinc300,
        animationSpec = StudioMotion.hoverSpec(),
        label = "reload-title"
    )
    val iconAlpha by animateFloatAsState(
        targetValue = if (hovered) 0.6f else 0.4f,
        animationSpec = StudioMotion.hoverSpec(),
        label = "reload-icon-alpha"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (success) 0f else 1f,
        animationSpec = StudioMotion.hoverSpec(),
        label = "reload-content-alpha"
    )
    val checkAlpha by animateFloatAsState(
        targetValue = if (success) 1f else 0f,
        animationSpec = StudioMotion.hoverSpec(),
        label = "reload-check-alpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor, CARD_SHAPE)
            .border(1.dp, borderColor, CARD_SHAPE)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = state == ReloadState.IDLE
            ) {
                ClientPayloadSender.send(ReloadRequestPayload())
                state = ReloadState.LOADING
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(contentAlpha)
        ) {
            SvgIcon(
                location = RELOAD_ICON,
                size = 18.dp,
                tint = Color.White,
                modifier = Modifier
                    .alpha(iconAlpha)
                    .rotate(rotation.value)
            )
            Spacer(modifier = Modifier.width(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = I18n.get("sidebar:reload.title"),
                    style = StudioTypography.medium(13),
                    color = titleColor
                )
                Text(
                    text = I18n.get("sidebar:reload.subtitle"),
                    style = StudioTypography.regular(10),
                    color = StudioColors.Zinc500
                )
            }
        }
        AnimatedCheckmark(
            progress = checkProgress.value,
            color = Color.White,
            size = 22.dp,
            modifier = Modifier.alpha(checkAlpha)
        )
    }
}


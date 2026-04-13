package fr.hardel.asset_editor.client.compose.components.layout.editor

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.network.ClientPayloadSender
import fr.hardel.asset_editor.network.ReloadRequestPayload
import kotlinx.coroutines.delay
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val RELOAD_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/reload.svg")
private val CHECK_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/check.svg")
private val CARD_SHAPE = RoundedCornerShape(8.dp)

private const val SPIN_DURATION_MS = 700
private const val COLOR_FADE_MS = 220
private const val ICON_FADE_MS = 180
private const val SUCCESS_HOLD_MS = 1600L

private enum class ReloadState { IDLE, LOADING, SUCCESS }

@Composable
fun StudioReloadButton(modifier: Modifier = Modifier) {
    var state by remember { mutableStateOf(ReloadState.IDLE) }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(state) {
        when (state) {
            ReloadState.LOADING -> {
                rotation.snapTo(0f)
                rotation.animateTo(
                    targetValue = 360f,
                    animationSpec = tween(SPIN_DURATION_MS, easing = FastOutSlowInEasing)
                )
                state = ReloadState.SUCCESS
            }
            ReloadState.SUCCESS -> {
                delay(SUCCESS_HOLD_MS)
                state = ReloadState.IDLE
            }
            ReloadState.IDLE -> rotation.snapTo(0f)
        }
    }

    val success = state == ReloadState.SUCCESS
    val backgroundColor by animateColorAsState(
        targetValue = when {
            success -> StudioColors.Zinc100
            hovered -> StudioColors.Zinc200
            else -> StudioColors.Zinc300
        },
        animationSpec = tween(COLOR_FADE_MS),
        label = "reload-bg"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            hovered -> StudioColors.Zinc500
            else -> StudioColors.Zinc400
        },
        animationSpec = tween(COLOR_FADE_MS),
        label = "reload-border"
    )
    val foregroundColor = StudioColors.Zinc950

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
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
        Crossfade(
            targetState = success,
            animationSpec = tween(ICON_FADE_MS),
            label = "reload-icon"
        ) { isSuccess ->
            if (isSuccess) {
                SvgIcon(CHECK_ICON, 18.dp, foregroundColor)
            } else {
                SvgIcon(
                    location = RELOAD_ICON,
                    size = 18.dp,
                    tint = foregroundColor,
                    modifier = Modifier.rotate(rotation.value)
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = I18n.get("sidebar:reload.title"),
                style = StudioTypography.medium(13),
                color = foregroundColor
            )
            Text(
                text = I18n.get("sidebar:reload.subtitle"),
                style = StudioTypography.regular(10),
                color = foregroundColor.copy(alpha = 0.55f)
            )
        }
    }
}

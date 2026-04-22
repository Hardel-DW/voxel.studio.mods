package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.slideEnterTransform
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTypography
import kotlinx.coroutines.delay
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val TOAST_SHAPE = RoundedCornerShape(10.dp)
private val WARNING_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/warning.svg")
private const val DISMISS_DELAY_MS = 4000L

class StudioToastState {
    var message by mutableStateOf<String?>(null)
        private set
    var trigger by mutableIntStateOf(0)
        private set

    fun show(translationKey: String) {
        message = translationKey
        trigger++
    }

    fun dismiss() {
        message = null
    }
}

@Composable
fun rememberToastState(): StudioToastState = remember { StudioToastState() }

@Composable
fun StudioToast(state: StudioToastState, modifier: Modifier = Modifier) {
    val message = state.message ?: return
    val progress = remember { Animatable(0f) }

    LaunchedEffect(state.trigger) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(StudioMotion.Short4, easing = StudioMotion.EmphasizedDecelerate))
        delay(DISMISS_DELAY_MS)
        progress.animateTo(0f, tween(StudioMotion.Short3, easing = StudioMotion.EmphasizedAccelerate))
        state.dismiss()
    }

    Box(
        modifier = modifier
            .slideEnterTransform(progress.value, translateY = 24.dp)
            .shadow(
                16.dp,
                TOAST_SHAPE,
                ambientColor = Color.Black.copy(alpha = 0.4f),
                spotColor = Color.Black.copy(alpha = 0.4f)
            )
            .border(1.dp, StudioColors.Zinc800.copy(alpha = 0.6f), TOAST_SHAPE)
            .background(StudioColors.Zinc900, TOAST_SHAPE)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SvgIcon(WARNING_ICON, 16.dp, StudioColors.Amber400, modifier = Modifier.size(16.dp))
            Text(
                text = I18n.get(message),
                style = StudioTypography.regular(12),
                color = StudioColors.Zinc300,
                modifier = Modifier.padding(start = 10.dp)
            )
        }
    }
}

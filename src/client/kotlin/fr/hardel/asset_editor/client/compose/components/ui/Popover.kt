package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import fr.hardel.asset_editor.client.compose.PopupEnterAnimation
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import fr.hardel.asset_editor.client.compose.StudioColors

@Composable
fun Popover(
    expanded: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    matchTriggerWidth: Boolean = false,
    alignment: Alignment = Alignment.TopStart,
    offset: IntOffset = IntOffset(0, 8),
    content: @Composable () -> Unit
) {
    if (!expanded) return

    val shape = RoundedCornerShape(16.dp)

    Popup(
        alignment = alignment,
        offset = offset,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        PopupEnterAnimation {
            Box(
                modifier = modifier
                    .then(if (matchTriggerWidth) Modifier.fillMaxWidth() else Modifier.widthIn(min = 160.dp))
                    .shadow(20.dp, shape, ambientColor = Color.Black.copy(alpha = 0.6f), spotColor = Color.Black.copy(alpha = 0.6f))
                    .border(1.dp, StudioColors.Zinc800, shape)
                    .background(StudioColors.Zinc950, shape)
                    .clip(shape)
            ) {
            ShineOverlay(
                modifier = Modifier.matchParentSize(),
                opacity = 0.12f,
                coverage = 0.35f
            )
            Box(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
        }
    }
}

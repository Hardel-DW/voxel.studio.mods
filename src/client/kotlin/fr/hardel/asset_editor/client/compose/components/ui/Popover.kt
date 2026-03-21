package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

@Composable
fun Popover(
    expanded: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    matchTriggerWidth: Boolean = false,
    content: @Composable () -> Unit
) {
    if (!expanded) return

    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(0, 8),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Box(
            modifier = modifier
                .then(if (matchTriggerWidth) Modifier.fillMaxWidth() else Modifier.widthIn(min = 160.dp))
                .clip(RoundedCornerShape(16.dp))
        ) {
            ShineOverlay(opacity = 0.12f)
            content()
        }
    }
}

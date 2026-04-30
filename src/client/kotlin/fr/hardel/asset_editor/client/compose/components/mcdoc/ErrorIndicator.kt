package fr.hardel.asset_editor.client.compose.components.mcdoc

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.AnchorBelowPopup
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.McdocTokens

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ErrorIndicator(message: String, modifier: Modifier = Modifier) {
    var hovered by remember { mutableStateOf(false) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(14.dp)
            .clip(CircleShape)
            .border(1.5.dp, McdocTokens.Error, CircleShape)
            .onPointerEvent(PointerEventType.Enter) { hovered = true }
            .onPointerEvent(PointerEventType.Exit) { hovered = false }
    ) {
        Text(
            text = "!",
            style = StudioTypography.bold(10),
            color = McdocTokens.Error
        )
        if (hovered) {
            Popup(popupPositionProvider = AnchorBelowPopup(6)) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 320.dp)
                        .clip(RoundedCornerShape(McdocTokens.Radius))
                        .background(McdocTokens.PopupBg, RoundedCornerShape(McdocTokens.Radius))
                        .border(1.dp, McdocTokens.Error, RoundedCornerShape(McdocTokens.Radius))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(text = message, style = StudioTypography.regular(12), color = McdocTokens.Text)
                }
            }
        }
    }
}

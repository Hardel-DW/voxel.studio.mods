package fr.hardel.asset_editor.client.compose.components.page.structure

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import net.minecraft.resources.Identifier

private val EYE_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/eye.svg")
private val RAIL_HEIGHT = 168.dp

@Composable
fun StructureYSlider(
    value: Int,
    max: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (max <= 1) return
    val shape = RoundedCornerShape(10.dp)
    val coerced = value.coerceIn(0, max)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .background(StudioColors.Zinc950.copy(alpha = 0.55f), shape)
            .border(1.dp, StudioColors.Zinc800.copy(alpha = 0.5f), shape)
            .padding(vertical = 8.dp, horizontal = 6.dp)
    ) {
        SvgIcon(EYE_ICON, 12.dp, StudioColors.Zinc500)
        Text("Y", style = StudioTypography.semiBold(10), color = StudioColors.Zinc500)
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(RAIL_HEIGHT)
                .pointerInput(max) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: continue
                            if (!change.pressed && event.type != PointerEventType.Scroll) continue
                            if (event.type == PointerEventType.Scroll) {
                                val delta = if (change.scrollDelta.y > 0f) -1 else 1
                                val next = (coerced + delta).coerceIn(0, max)
                                if (next != coerced) onValueChange(next)
                                change.consume()
                                continue
                            }
                            val ratio = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                            val next = (ratio * max).toInt().coerceIn(0, max)
                            if (next != coerced) onValueChange(next)
                            change.consume()
                        }
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(StudioColors.Zinc800)
            )
            val fillFraction = if (max == 0) 0f else coerced.toFloat() / max.toFloat()
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .width(2.dp)
                    .fillMaxHeight(fillFraction)
                    .background(StudioColors.Violet500)
            )
            val knobOffset = (RAIL_HEIGHT.value * (1f - fillFraction)).dp - 5.dp
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = knobOffset.coerceAtLeast(0.dp))
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(StudioColors.Zinc100)
            )
        }
        Text(coerced.toString(), style = StudioTypography.medium(10), color = StudioColors.Zinc300)
        Text("/${max}", style = StudioTypography.regular(9), color = StudioColors.Zinc500)
    }
}

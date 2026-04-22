package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.data.component.ComponentWidget

private val trackShape = RoundedCornerShape(12.dp)

@Composable
fun BooleanWidget(
    widget: ComponentWidget.BooleanWidget,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val current = remember(value, widget) { value?.asBooleanOrNull() ?: widget.defaultValue().orElse(false) }
    val interaction = remember { MutableInteractionSource() }

    val trackBg by animateColorAsState(
        targetValue = if (current) StudioColors.Violet500 else StudioColors.Zinc700,
        animationSpec = StudioMotion.hoverSpec(),
        label = "boolean-widget-track"
    )
    val knobOffsetX by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (current) 20.dp else 2.dp,
        animationSpec = StudioMotion.hoverSpec(),
        label = "boolean-widget-knob"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(24.dp)
                .clip(trackShape)
                .background(trackBg, trackShape)
                .hoverable(interaction)
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = { onValueChange(JsonPrimitive(!current)) }
                )
        ) {
            Box(
                modifier = Modifier
                    .offset(x = knobOffsetX)
                    .align(Alignment.CenterStart)
                    .size(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudioColors.Zinc50, RoundedCornerShape(10.dp))
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = if (current) "true" else "false",
            style = StudioTypography.regular(12),
            color = StudioColors.Zinc500
        )
    }
}

private fun JsonElement.asBooleanOrNull(): Boolean? =
    runCatching { asBoolean }.getOrNull()

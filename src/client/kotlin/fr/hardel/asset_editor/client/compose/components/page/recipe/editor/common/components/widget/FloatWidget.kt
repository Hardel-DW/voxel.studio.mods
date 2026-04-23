package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.data.component.ComponentWidget

private val shape = RoundedCornerShape(8.dp)

@Composable
fun FloatWidget(
    widget: ComponentWidget.FloatWidget,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentFloat = remember(value) { value?.asFloatOrNull() }
    var text by remember(value) { mutableStateOf(TextFieldValue(currentFloat?.toPrettyString().orEmpty())) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .height(32.dp)
                .width(120.dp)
                .clip(shape)
                .background(StudioColors.Zinc950.copy(alpha = 0.5f), shape)
                .border(1.dp, StudioColors.Zinc800, shape)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = text,
                onValueChange = { next ->
                    text = next
                    val parsed = next.text.toFloatOrNull() ?: return@BasicTextField
                    val clamped = parsed
                        .coerceAtLeast(widget.min().orElse(-Float.MAX_VALUE))
                        .coerceAtMost(widget.max().orElse(Float.MAX_VALUE))
                    onValueChange(JsonPrimitive(clamped))
                },
                textStyle = StudioTypography.regular(13).copy(color = StudioColors.Zinc100),
                cursorBrush = SolidColor(StudioColors.Zinc100),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.width(12.dp))
        RangeHint(widget)
    }
}

@Composable
private fun RangeHint(widget: ComponentWidget.FloatWidget) {
    val min = widget.min().orElse(null)
    val max = widget.max().orElse(null)
    val text = when {
        min != null && max != null -> "${min.toPrettyString()} – ${max.toPrettyString()}"
        min != null -> "≥ ${min.toPrettyString()}"
        max != null -> "≤ ${max.toPrettyString()}"
        else -> return
    }
    Text(
        text = text,
        style = StudioTypography.regular(11),
        color = StudioColors.Zinc500
    )
}

private fun JsonElement.asFloatOrNull(): Float? = runCatching { asFloat }.getOrNull()

private fun Float.toPrettyString(): String {
    if (this == this.toInt().toFloat()) return this.toInt().toString()
    return this.toString()
}

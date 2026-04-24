package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.common.FieldRowHeight
import fr.hardel.asset_editor.data.component.ComponentWidget
import net.minecraft.client.resources.language.I18n

private val shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)

@Composable
fun StringWidget(
    widget: ComponentWidget.StringWidget,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val initial = remember { value?.asStringOrNull().orEmpty() }
    var text by remember { mutableStateOf(TextFieldValue(initial)) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(FieldRowHeight)
            .clip(shape)
            .background(StudioColors.Zinc950.copy(alpha = 0.5f), shape)
            .border(1.dp, StudioColors.Zinc800, shape)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (text.text.isEmpty()) {
            Text(
                text = I18n.get("recipe:components.widget.unset"),
                style = StudioTypography.regular(12),
                color = StudioColors.Zinc600
            )
        }
        BasicTextField(
            value = text,
            onValueChange = { next ->
                val maxLength = widget.maxLength().orElse(Int.MAX_VALUE)
                val trimmed = if (next.text.length > maxLength) next.copy(text = next.text.take(maxLength)) else next
                text = trimmed
                onValueChange(JsonPrimitive(trimmed.text))
            },
            textStyle = StudioTypography.regular(12).copy(color = StudioColors.Zinc100),
            cursorBrush = SolidColor(StudioColors.Zinc100),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun JsonElement.asStringOrNull(): String? = runCatching { asString }.getOrNull()

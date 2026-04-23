package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography

private val shape = RoundedCornerShape(8.dp)

@Composable
fun IdentifierWidget(
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val current = remember(value) { runCatching { value?.asString }.getOrNull().orEmpty() }
    var text by remember(value) { mutableStateOf(TextFieldValue(current)) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
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
                onValueChange(JsonPrimitive(next.text))
            },
            textStyle = StudioTypography.regular(13)
                .copy(color = StudioColors.Zinc100, fontFamily = FontFamily.Monospace),
            cursorBrush = SolidColor(StudioColors.Zinc100),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

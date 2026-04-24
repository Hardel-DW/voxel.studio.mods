package fr.hardel.asset_editor.client.compose.components.codec.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.codec.CodecTokens

private val shape = RoundedCornerShape(CodecTokens.RadiusLg)

@Composable
fun RawJsonWidget(
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val initial = remember(value) { value?.toString().orEmpty() }
    var text by remember(initial) { mutableStateOf(TextFieldValue(initial)) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CodecTokens.InputBg, shape)
            .border(1.dp, CodecTokens.Border, shape)
            .padding(10.dp)
    ) {
        BasicTextField(
            value = text,
            onValueChange = { next ->
                text = next
                runCatching { JsonParser.parseString(next.text) }
                    .getOrNull()
                    ?.let(onValueChange)
            },
            textStyle = StudioTypography.regular(12).copy(color = CodecTokens.Text),
            cursorBrush = SolidColor(CodecTokens.Text),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

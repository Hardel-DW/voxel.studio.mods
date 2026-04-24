package fr.hardel.asset_editor.client.compose.components.codec.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.codec.CodecTokens
import net.minecraft.client.resources.language.I18n

private val shape = RoundedCornerShape(CodecTokens.RadiusLg)

@Composable
fun TextCodecWidget(
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val initial = remember { extractPlainText(value) }
    var text by remember { mutableStateOf(TextFieldValue(initial)) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clip(shape)
                .background(CodecTokens.InputBg, shape)
                .border(1.dp, CodecTokens.Border, shape)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = text,
                onValueChange = { next ->
                    text = next
                    onValueChange(JsonPrimitive(next.text))
                },
                textStyle = StudioTypography.regular(13).copy(color = CodecTokens.Text),
                cursorBrush = SolidColor(CodecTokens.Text),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Text(
            text = I18n.get("codec:text_component_hint"),
            style = StudioTypography.regular(10),
            color = CodecTokens.TextMuted
        )
    }
}

private fun extractPlainText(value: JsonElement?): String {
    if (value == null || value.isJsonNull) return ""
    if (value.isJsonPrimitive && value.asJsonPrimitive.isString) return value.asString
    if (value.isJsonObject) {
        val obj = value.asJsonObject
        val textField = obj.get("text")
        if (textField?.isJsonPrimitive == true && textField.asJsonPrimitive.isString) {
            return textField.asString
        }
    }
    return ""
}

package fr.hardel.asset_editor.client.compose.components.mcdoc.heads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.McdocTextInput

@Composable
fun AnyHead(
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val current = remember(value) { value?.toString().orEmpty() }
    McdocTextInput(
        value = current,
        onValueChange = { next ->
            runCatching { JsonParser.parseString(next) }.getOrNull()?.let(onValueChange)
        },
        placeholder = "{ raw json }",
        modifier = modifier
    )
}

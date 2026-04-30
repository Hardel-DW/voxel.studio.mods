package fr.hardel.asset_editor.client.compose.components.mcdoc.bodies

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import fr.hardel.asset_editor.client.compose.components.mcdoc.ErrorIndicator
import fr.hardel.asset_editor.client.compose.components.mcdoc.Key
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.McdocTokens
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.RemoveIconButton
import net.minecraft.client.resources.language.I18n

@Composable
fun UnknownField(
    key: String,
    obj: JsonObject,
    onObjectChange: (JsonElement) -> Unit
) {
    val removeField = { onObjectChange(obj.deepCopy().apply { remove(key) }) }
    val message = I18n.get("mcdoc:error.unknown_key").replace("{0}", key)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Key(label = key, raw = true, color = McdocTokens.Error)
        Spacer(Modifier.weight(1f))
        RemoveIconButton(onClick = removeField)
        Spacer(Modifier.widthIn(min = 6.dp))
        ErrorIndicator(message = message)
    }
}

package fr.hardel.asset_editor.client.compose.components.page.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.CopyButton
import fr.hardel.asset_editor.client.memory.session.debug.DebugLogMemory
import net.minecraft.client.resources.language.I18n

@Composable
fun DebugLogsExpandRow(entry: DebugLogMemory.Entry) {
    if (entry.data().isEmpty()) {
        Text(
            text = I18n.get("debug:logs.no_additional_data"),
            style = StudioTypography.regular(12),
            color = StudioColors.Zinc500
        )
        return
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        entry.data().forEach { (key, value) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(key, style = StudioTypography.medium(11), color = StudioColors.Zinc500)
                Text(
                    value,
                    style = StudioTypography.regular(12),
                    color = StudioColors.Zinc300,
                    modifier = Modifier.weight(1f)
                )
                CopyButton(textProvider = { value })
            }
        }
    }
}

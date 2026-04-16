package fr.hardel.asset_editor.client.compose.components.page.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.Button
import fr.hardel.asset_editor.client.compose.components.ui.ButtonSize
import fr.hardel.asset_editor.client.compose.components.ui.ButtonVariant
import net.minecraft.client.resources.language.I18n

@Composable
fun DebugLogsActionBar(
    entryCount: Int,
    onCopyAll: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = I18n.get("debug:logs.count", entryCount),
            style = StudioTypography.regular(12),
            color = StudioColors.Zinc500
        )
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onCopyAll,
            variant = ButtonVariant.GHOST_BORDER,
            size = ButtonSize.SM,
            text = I18n.get("debug:logs.action.copy_all")
        )
        Button(
            onClick = onClear,
            variant = ButtonVariant.GHOST_BORDER,
            size = ButtonSize.SM,
            text = I18n.get("debug:action.clear")
        )
    }
}

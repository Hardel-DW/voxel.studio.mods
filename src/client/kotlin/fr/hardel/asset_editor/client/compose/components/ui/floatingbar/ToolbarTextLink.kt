package fr.hardel.asset_editor.client.compose.components.ui.floatingbar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.minecraft.resources.Identifier

@Composable
fun ToolbarTextLink(
    icon: Identifier,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    ToolbarTextButton(icon = icon, label = label, onClick = onClick, modifier = modifier, enabled = enabled)
}

package fr.hardel.asset_editor.client.compose.components.page.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import fr.hardel.asset_editor.client.compose.components.ui.DropdownMenu
import fr.hardel.asset_editor.client.compose.components.ui.DropdownMenuContent
import fr.hardel.asset_editor.client.compose.components.ui.DropdownMenuItem
import fr.hardel.asset_editor.client.compose.components.ui.DropdownMenuSelectTrigger
import net.minecraft.client.resources.language.I18n

@Composable
fun DebugNetworkNormalActionBar(
    entryCount: Int,
    namespaces: List<String>,
    selectedNamespace: String,
    onNamespaceChange: (String) -> Unit,
    onCopyAll: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp)
    ) {
        Text(
            text = I18n.get("debug:network.count", entryCount),
            style = StudioTypography.regular(12),
            color = StudioColors.Zinc500
        )
        DropdownMenu {
            DropdownMenuSelectTrigger(label = selectedNamespace)
            DropdownMenuContent {
                namespaces.forEach { ns ->
                    DropdownMenuItem(onClick = { onNamespaceChange(ns) }) {
                        Text(text = ns, style = StudioTypography.regular(13))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onCopyAll,
            variant = ButtonVariant.GHOST_BORDER,
            size = ButtonSize.SM,
            text = I18n.get("debug:action.copy_all")
        )
        Button(
            onClick = onClear,
            variant = ButtonVariant.GHOST_BORDER,
            size = ButtonSize.SM,
            text = I18n.get("debug:action.clear")
        )
    }
}

@Composable
fun DebugNetworkSelectionActionBar(
    selectedCount: Int,
    onCopySelected: () -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp)
    ) {
        Text(
            text = I18n.get("debug:network.selected", selectedCount),
            style = StudioTypography.medium(12),
            color = StudioColors.Zinc300
        )
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onCopySelected,
            variant = ButtonVariant.GHOST_BORDER,
            size = ButtonSize.SM,
            text = I18n.get("debug:action.copy_selected")
        )
        Button(
            onClick = onSelectAll,
            variant = ButtonVariant.GHOST_BORDER,
            size = ButtonSize.SM,
            text = I18n.get("debug:action.select_all")
        )
        Button(
            onClick = onDeselectAll,
            variant = ButtonVariant.GHOST_BORDER,
            size = ButtonSize.SM,
            text = I18n.get("debug:action.deselect_all")
        )
        Button(
            onClick = onDeleteSelected,
            variant = ButtonVariant.GHOST_BORDER,
            size = ButtonSize.SM,
            text = I18n.get("debug:action.delete_selected")
        )
    }
}

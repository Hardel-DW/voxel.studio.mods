package fr.hardel.asset_editor.client.compose.components.page.structure

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.network.structure.StructureAssemblyParameters
import net.minecraft.resources.Identifier

private val RELOAD_ICON: Identifier = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/reload.svg")
private val LOCATE_ICON: Identifier = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/locate.svg")
private val SETTINGS_ICON: Identifier = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/settings.svg")

@Composable
fun StructureVariantControls(
    state: StructureVariantState,
    current: StructureAssemblyParameters,
    locateEnabled: Boolean,
    onLocate: () -> Unit
) {
    var popoverOpen by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        OverlayIconButton(
            icon = RELOAD_ICON,
            iconSize = 12,
            boxSize = 24,
            onClick = { state.reroll(current.chunkX(), current.chunkZ()) }
        )
        Spacer(Modifier.width(4.dp))
        OverlayIconButton(
            icon = LOCATE_ICON,
            iconSize = 12,
            boxSize = 24,
            enabled = locateEnabled,
            onClick = onLocate
        )
        Spacer(Modifier.width(4.dp))
        Box {
            OverlayIconButton(
                icon = SETTINGS_ICON,
                iconSize = 12,
                boxSize = 24,
                onClick = { popoverOpen = !popoverOpen }
            )
            StructureVariantPopover(
                expanded = popoverOpen,
                onDismiss = { popoverOpen = false },
                current = current,
                counter = state.counter,
                onApply = { seed, chunkX, chunkZ -> state.apply(seed, chunkX, chunkZ) },
                onReset = { state.reset() }
            )
        }
    }
}

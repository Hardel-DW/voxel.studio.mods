package fr.hardel.asset_editor.client.compose.components.page.structure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.Button
import fr.hardel.asset_editor.client.compose.components.ui.ButtonSize
import fr.hardel.asset_editor.client.compose.components.ui.ButtonVariant
import fr.hardel.asset_editor.client.compose.components.ui.InputText
import fr.hardel.asset_editor.client.compose.components.ui.Popover
import fr.hardel.asset_editor.network.structure.StructureAssemblyParameters
import net.minecraft.client.resources.language.I18n

@Composable
fun StructureVariantPopover(
    expanded: Boolean,
    onDismiss: () -> Unit,
    current: StructureAssemblyParameters,
    counter: Int,
    onApply: (seed: Long, chunkX: Int, chunkZ: Int) -> Unit,
    onReset: () -> Unit
) {
    var seedField by remember(expanded, current.seed()) { mutableStateOf(current.seed().toString()) }
    var chunkXField by remember(expanded, current.chunkX()) { mutableStateOf(current.chunkX().toString()) }
    var chunkZField by remember(expanded, current.chunkZ()) { mutableStateOf(current.chunkZ().toString()) }

    val parsedSeed = seedField.trim().toLongOrNull()
    val parsedChunkX = chunkXField.trim().toIntOrNull()
    val parsedChunkZ = chunkZField.trim().toIntOrNull()
    val applyEnabled = parsedSeed != null && parsedChunkX != null && parsedChunkZ != null
        && (parsedSeed != current.seed() || parsedChunkX != current.chunkX() || parsedChunkZ != current.chunkZ())

    Popover(
        expanded = expanded,
        onDismiss = onDismiss,
        alignment = Alignment.TopEnd,
        offset = IntOffset(0, 8),
        modifier = Modifier.widthIn(min = 300.dp, max = 320.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            VariantHeader(counter)

            FieldLabel(I18n.get("structure:variant.seed"))
            InputText(
                value = seedField,
                onValueChange = { seedField = it },
                showSearchIcon = false,
                focusExpand = false,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel(I18n.get("structure:variant.chunk_x"))
                    InputText(
                        value = chunkXField,
                        onValueChange = { chunkXField = it },
                        showSearchIcon = false,
                        focusExpand = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel(I18n.get("structure:variant.chunk_z"))
                    InputText(
                        value = chunkZField,
                        onValueChange = { chunkZField = it },
                        showSearchIcon = false,
                        focusExpand = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onReset(); onDismiss() },
                    variant = ButtonVariant.GHOST_BORDER,
                    size = ButtonSize.SM,
                    text = I18n.get("structure:variant.reset"),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        if (parsedSeed != null && parsedChunkX != null && parsedChunkZ != null) {
                            onApply(parsedSeed, parsedChunkX, parsedChunkZ)
                            onDismiss()
                        }
                    },
                    variant = ButtonVariant.DEFAULT,
                    size = ButtonSize.SM,
                    text = I18n.get("structure:variant.apply"),
                    enabled = applyEnabled,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun VariantHeader(counter: Int) {
    val label = if (counter > 0) {
        I18n.get("structure:variant.indexed", counter)
    } else {
        I18n.get("structure:variant.world_default")
    }
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Text(
            text = label,
            style = StudioTypography.semiBold(13),
            color = StudioColors.Zinc100
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = StudioTypography.medium(11),
        color = StudioColors.Zinc400
    )
}

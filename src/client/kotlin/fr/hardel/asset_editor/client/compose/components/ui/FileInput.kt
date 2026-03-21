package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
fun FileInput(
    promptText: String,
    onFileSelected: (File) -> Unit,
    modifier: Modifier = Modifier,
    accept: String? = null
) {
    var selectedFileName by remember { mutableStateOf<String?>(null) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .border(
                width = 2.dp,
                color = VoxelColors.Zinc700.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                val dialog = FileDialog(null as Frame?, "Select file", FileDialog.LOAD)
                if (accept != null) dialog.file = accept
                dialog.isVisible = true
                val dir = dialog.directory
                val file = dialog.file
                if (dir != null && file != null) {
                    val selected = File(dir, file)
                    selectedFileName = selected.name
                    onFileSelected(selected)
                }
            }
    ) {
        Text(
            text = selectedFileName ?: promptText,
            style = VoxelTypography.regular(12),
            color = if (selectedFileName != null) VoxelColors.Zinc200 else VoxelColors.Zinc500
        )
    }
}

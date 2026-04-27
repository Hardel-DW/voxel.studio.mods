package fr.hardel.asset_editor.client.compose.components.page.structure

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.components.ui.Pagination
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val RECENTER_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/recenter.svg")

@Composable
fun StructureBottomOverlay(
    showJigsaws: Boolean,
    onShowJigsawsChange: (Boolean) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    showStageControls: Boolean = false,
    step: Int = 0,
    maxStep: Int = 0,
    onStepChange: (Int) -> Unit = {},
    showPoolToggle: Boolean = false,
    showPoolBoxes: Boolean = false,
    onShowPoolBoxesChange: (Boolean) -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(horizontal = 2.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (showStageControls && maxStep > 0) {
                Pagination(
                    currentPage = step,
                    totalPages = maxStep + 1,
                    onPageChange = onStepChange,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        OverlayCheckbox(
            label = I18n.get("structure:overlay.jigsaws"),
            checked = showJigsaws,
            onCheckedChange = onShowJigsawsChange
        )
        if (showPoolToggle) {
            Spacer(Modifier.width(12.dp))
            OverlayCheckbox(
                label = I18n.get("structure:overlay.boxes"),
                checked = showPoolBoxes,
                onCheckedChange = onShowPoolBoxesChange
            )
        }
        Spacer(Modifier.width(12.dp))
        OverlayDivider()
        Spacer(Modifier.width(8.dp))
        OverlayIconButton(RECENTER_ICON, iconSize = 12, boxSize = 24, onClick = onReset)
    }
}

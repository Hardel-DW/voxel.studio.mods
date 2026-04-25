package fr.hardel.asset_editor.client.compose.components.page.structure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val RELOAD_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/reload.svg")
private val ARROW_LEFT = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/arrow-left.svg")
private val ARROW_RIGHT = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/arrow-right.svg")

@Composable
fun StructureBottomOverlay(
    viewMode: StructureViewMode,
    step: Int,
    maxStep: Int,
    onStepChange: (Int) -> Unit,
    animations: Boolean,
    onAnimationsChange: (Boolean) -> Unit,
    showJigsaws: Boolean,
    onShowJigsawsChange: (Boolean) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isStructureMode = viewMode == StructureViewMode.STRUCTURE
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (isStructureMode && maxStep > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    OverlayIconButton(ARROW_LEFT, iconSize = 11, enabled = step > 0) { onStepChange(step - 1) }
                    Text(
                        text = "${step}/${maxStep}",
                        style = StudioTypography.medium(11),
                        color = StudioColors.Zinc300,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .width(44.dp)
                    )
                    OverlayIconButton(ARROW_RIGHT, iconSize = 11, enabled = step < maxStep) { onStepChange(step + 1) }
                }
            }
        }

        if (isStructureMode) {
            OverlayCheckbox(
                label = I18n.get("structure:overlay.animations"),
                checked = animations,
                onCheckedChange = onAnimationsChange
            )
            Spacer(Modifier.width(12.dp))
        }
        OverlayCheckbox(
            label = I18n.get("structure:overlay.jigsaws"),
            checked = showJigsaws,
            onCheckedChange = onShowJigsawsChange
        )
        Spacer(Modifier.width(12.dp))
        OverlayDivider()
        Spacer(Modifier.width(8.dp))
        OverlayIconButton(RELOAD_ICON, iconSize = 12, boxSize = 24, onClick = onReset)
    }
}

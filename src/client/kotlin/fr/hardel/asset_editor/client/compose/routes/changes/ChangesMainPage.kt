package fr.hardel.asset_editor.client.compose.routes.changes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import fr.hardel.asset_editor.client.compose.components.ui.GridBackground
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val PENCIL_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/pencil.svg")

@Composable
fun ChangesMainPage(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VoxelColors.Zinc960)
    ) {
        GridBackground(modifier = Modifier.matchParentSize())

        Canvas(modifier = Modifier.matchParentSize()) {
            val step = 32.dp.toPx()
            var x = -size.height
            while (x < size.width + size.height) {
                drawLine(
                    color = Color.White.copy(alpha = 0.08f),
                    start = androidx.compose.ui.geometry.Offset(x, 0f),
                    end = androidx.compose.ui.geometry.Offset(x + size.height, size.height),
                    strokeWidth = 1f
                )
                x += step
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(96.dp)
                    .background(VoxelColors.Zinc900.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
            ) {
                SvgIcon(PENCIL_ICON, 40.dp, Color.White.copy(alpha = 0.2f))
            }

            Text(
                text = I18n.get("changes:emptystate.title"),
                style = VoxelTypography.medium(20),
                color = VoxelColors.Zinc300
            )

            Text(
                text = I18n.get("changes:emptystate.description"),
                style = VoxelTypography.regular(14),
                color = VoxelColors.Zinc500
            )
        }
    }
}

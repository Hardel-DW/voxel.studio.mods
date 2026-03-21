package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import net.minecraft.resources.Identifier

@Composable
fun TemplateCard(
    iconPath: Identifier,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    child: (@Composable () -> Unit)? = null
) {
    SimpleCard(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            SvgIcon(iconPath, 64.dp, Color.White)
            Spacer(Modifier.weight(1f))
            child?.invoke()
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Text(
                text = title,
                style = VoxelTypography.semiBold(18),
                color = VoxelColors.Zinc100
            )
            if (description != null) {
                Text(
                    text = description,
                    style = VoxelTypography.regular(14),
                    color = VoxelColors.Zinc400
                )
            }
        }
    }
}

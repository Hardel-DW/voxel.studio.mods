package fr.hardel.asset_editor.client.compose.components.codec.widget

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import net.minecraft.client.resources.language.I18n

@Composable
fun UnitWidget(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = I18n.get("codec:unit_flag"),
            style = StudioTypography.regular(12),
            color = StudioColors.Zinc400
        )
    }
}

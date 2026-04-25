package fr.hardel.asset_editor.client.compose.components.page.structure

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.Button
import fr.hardel.asset_editor.client.compose.components.ui.ButtonVariant
import fr.hardel.asset_editor.client.compose.components.ui.ItemSprite
import fr.hardel.asset_editor.network.structure.StructureBlockCount

@Composable
fun BlockCountRow(count: StructureBlockCount, onPick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .background(StudioColors.Zinc900.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
            .border(1.dp, StudioColors.Zinc800, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ItemSprite(count.blockId(), 16.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            text = count.blockId().path,
            style = StudioTypography.regular(11),
            color = StudioColors.Zinc200,
            modifier = Modifier.weight(1f)
        )
        Button(
            onClick = onPick,
            variant = ButtonVariant.LINK,
            text = count.count().toString()
        )
    }
}

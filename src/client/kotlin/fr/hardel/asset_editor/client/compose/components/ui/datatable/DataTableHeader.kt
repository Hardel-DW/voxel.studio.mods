package fr.hardel.asset_editor.client.compose.components.ui.datatable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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

@Composable
internal fun <T> DataTableHeader(columns: List<TableColumn<T>>, showCheckboxColumn: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .background(StudioColors.Zinc900.copy(alpha = 0.4f), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            .padding(horizontal = 16.dp)
    ) {
        if (showCheckboxColumn) {
            Box(modifier = Modifier.width(32.dp))
        }
        for (col in columns) {
            Box(modifier = Modifier.weight(col.weight)) {
                Text(
                    text = col.header.uppercase(),
                    style = StudioTypography.medium(11),
                    color = StudioColors.Zinc500
                )
            }
        }
    }
}

package fr.hardel.asset_editor.client.compose.components.page.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.memory.session.debug.NetworkTraceMemory

@Composable
fun DebugNetworkDirectionBadge(direction: NetworkTraceMemory.Direction) {
    val inbound = direction == NetworkTraceMemory.Direction.INBOUND

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = if (inbound) "S" else "C",
            style = StudioTypography.semiBold(11),
            color = if (inbound) StudioColors.Red400 else StudioColors.Zinc300
        )
        Text("->", style = StudioTypography.medium(11), color = StudioColors.Zinc600)
        Text(
            text = if (inbound) "C" else "S",
            style = StudioTypography.semiBold(11),
            color = if (inbound) StudioColors.Zinc300 else StudioColors.Red400
        )
    }
}

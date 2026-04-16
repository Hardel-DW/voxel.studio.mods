package fr.hardel.asset_editor.client.compose.components.page.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.CopyButton
import fr.hardel.asset_editor.client.compose.components.ui.KeyValueGrid
import fr.hardel.asset_editor.client.memory.session.debug.NetworkTraceMemory
import net.minecraft.client.resources.language.I18n
import java.time.Instant

@Composable
fun DebugNetworkTraceExpand(entry: NetworkTraceMemory.TraceEntry) {
    val inbound = entry.direction() == NetworkTraceMemory.Direction.INBOUND
    val directionLabel = if (inbound) "Server -> Client" else "Client -> Server"
    val directionColor = if (inbound) StudioColors.Red400 else StudioColors.Zinc300

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(StudioColors.Zinc900.copy(alpha = 0.3f))
            .border(1.dp, StudioColors.Zinc800.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = directionLabel,
                style = StudioTypography.semiBold(11),
                color = directionColor,
                modifier = Modifier
                    .background(directionColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
            Text(
                text = entry.payloadId().toString(),
                style = StudioTypography.medium(12),
                color = StudioColors.Zinc300
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = DEBUG_TIME_FORMAT.format(Instant.ofEpochMilli(entry.timestamp())),
                style = StudioTypography.regular(11),
                color = StudioColors.Zinc500
            )
            CopyButton(
                iconSize = 14.dp,
                textProvider = { serializeNetworkEntry(entry) }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(StudioColors.Zinc800.copy(alpha = 0.5f))
        )

        val payload = entry.payload()
        if (payload == null || !payload.javaClass.isRecord) {
            Text(
                text = I18n.get("debug:network.no_additional_data"),
                style = StudioTypography.regular(12),
                color = StudioColors.Zinc500
            )
        } else {
            KeyValueGrid(payload)
        }
    }
}

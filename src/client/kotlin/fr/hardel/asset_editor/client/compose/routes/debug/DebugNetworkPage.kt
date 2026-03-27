package fr.hardel.asset_editor.client.compose.routes.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import fr.hardel.asset_editor.client.compose.components.ui.Button
import fr.hardel.asset_editor.client.compose.components.ui.ButtonSize
import fr.hardel.asset_editor.client.compose.components.ui.ButtonVariant
import fr.hardel.asset_editor.client.compose.components.ui.CopyButton
import fr.hardel.asset_editor.client.compose.components.ui.DataTable
import fr.hardel.asset_editor.client.compose.components.ui.Dropdown
import fr.hardel.asset_editor.client.compose.components.ui.KeyValueGrid
import fr.hardel.asset_editor.client.compose.components.ui.TableColumn
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.asFlow
import fr.hardel.asset_editor.client.memory.debug.NetworkTraceMemory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import net.minecraft.client.resources.language.I18n

private val NETWORK_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault())

@Composable
fun DebugNetworkPage(context: StudioContext) {
    var currentPage by remember { mutableStateOf(0) }
    var expandedIds by remember { mutableStateOf(emptySet<Long>()) }
    val flow = remember(context) { context.debugMemory().network().asFlow() }
    val snapshot by flow.collectAsState(context.debugMemory().network().snapshot())
    val allLabel = I18n.get("generic:all")
    val selectedNamespace = snapshot.selectedNamespace ?: allLabel
    val entries = snapshot.entries
    val namespaces = remember(snapshot.availableNamespaces, allLabel) {
        buildList {
            add(allLabel)
            addAll(snapshot.availableNamespaces)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = I18n.get("debug:network.count", entries.size),
                style = VoxelTypography.regular(12),
                color = VoxelColors.Zinc500
            )
            Dropdown(
                items = namespaces,
                selected = selectedNamespace,
                labelExtractor = { value -> value },
                onSelect = { namespace ->
                    context.debugMemory().network().selectNamespace(if (namespace == allLabel) null else namespace)
                }
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = context.debugMemory().network()::clear,
                variant = ButtonVariant.GHOST_BORDER,
                size = ButtonSize.SM,
                text = I18n.get("debug:action.clear")
            )
        }

        DataTable(
            items = entries,
            columns = listOf(
                TableColumn(I18n.get("debug:network.column.time"), weight = 0.8f) { entry ->
                    Text(NETWORK_TIME_FORMAT.format(Instant.ofEpochMilli(entry.timestamp())), style = VoxelTypography.regular(11), color = VoxelColors.Zinc500)
                },
                TableColumn(I18n.get("debug:network.column.direction"), weight = 0.7f) { entry ->
                    DirectionBadge(entry.direction())
                },
                TableColumn(I18n.get("debug:network.column.payload_id"), weight = 1.4f) { entry ->
                    Text(entry.payloadId().path, style = VoxelTypography.medium(11), color = VoxelColors.Zinc400)
                },
                TableColumn(I18n.get("debug:network.column.title"), weight = 1.4f) { entry ->
                    val titleKey = "debug:payload.${entry.payloadId().namespace}.${entry.payloadId().path.replace('/', '.')}.title"
                    Text(if (I18n.exists(titleKey)) I18n.get(titleKey) else entry.payloadId().path, style = VoxelTypography.regular(13), color = VoxelColors.Zinc300)
                },
                TableColumn(I18n.get("debug:network.column.description"), weight = 2.2f) { entry ->
                    val descKey = "debug:payload.${entry.payloadId().namespace}.${entry.payloadId().path.replace('/', '.')}.description"
                    Text(if (I18n.exists(descKey)) I18n.get(descKey) else "", style = VoxelTypography.regular(11), color = VoxelColors.Zinc500)
                },
                TableColumn("", weight = 0.35f) { entry ->
                    CopyButton(
                        iconSize = 14.dp,
                        textProvider = {
                            "{\"id\":${entry.id()},\"timestamp\":${entry.timestamp()},\"direction\":\"${entry.direction()}\",\"payloadId\":\"${entry.payloadId()}\"}"
                        }
                    )
                }
            ),
            pageSize = 50,
            currentPage = currentPage,
            onPageChange = { page -> currentPage = page },
            placeholder = I18n.get("debug:network.placeholder"),
            idExtractor = { entry -> entry.id() },
            expandedIds = expandedIds,
            onToggleExpand = { id ->
                expandedIds = if (expandedIds.contains(id)) expandedIds - id else expandedIds + id
            },
            expandContent = { entry -> TraceExpandContent(entry) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )
    }
}

@Composable
private fun DirectionBadge(direction: NetworkTraceMemory.Direction) {
    val inbound = direction == NetworkTraceMemory.Direction.INBOUND
    val color = if (inbound) VoxelColors.Red400 else VoxelColors.Emerald400

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = if (inbound) "S" else "C",
            style = VoxelTypography.semiBold(11),
            color = if (inbound) VoxelColors.Red400 else VoxelColors.Emerald400
        )
        Text("->", style = VoxelTypography.medium(11), color = VoxelColors.Zinc600)
        Text(
            text = if (inbound) "C" else "S",
            style = VoxelTypography.semiBold(11),
            color = if (inbound) VoxelColors.Emerald400 else VoxelColors.Red400
        )
    }
}

@Composable
private fun TraceExpandContent(entry: NetworkTraceMemory.TraceEntry) {
    val inbound = entry.direction() == NetworkTraceMemory.Direction.INBOUND
    val directionLabel = if (inbound) "Server -> Client" else "Client -> Server"
    val directionColor = if (inbound) VoxelColors.Red400 else VoxelColors.Emerald400

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(VoxelColors.Zinc900.copy(alpha = 0.3f))
            .border(1.dp, VoxelColors.Zinc800.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = directionLabel,
                style = VoxelTypography.semiBold(11),
                color = directionColor,
                modifier = Modifier
                    .background(directionColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
            Text(
                text = entry.payloadId().toString(),
                style = VoxelTypography.medium(12),
                color = VoxelColors.Zinc300
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = NETWORK_TIME_FORMAT.format(Instant.ofEpochMilli(entry.timestamp())),
                style = VoxelTypography.regular(11),
                color = VoxelColors.Zinc500
            )
            CopyButton(
                iconSize = 14.dp,
                textProvider = {
                    "{\"id\":${entry.id()},\"timestamp\":${entry.timestamp()},\"direction\":\"${entry.direction()}\",\"payloadId\":\"${entry.payloadId()}\"}"
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(VoxelColors.Zinc800.copy(alpha = 0.5f))
        )

        val payload = entry.payload()
        if (payload == null || !payload.javaClass.isRecord) {
            Text(
                text = I18n.get("debug:network.no_additional_data"),
                style = VoxelTypography.regular(12),
                color = VoxelColors.Zinc500
            )
        } else {
            KeyValueGrid(payload)
        }
    }
}

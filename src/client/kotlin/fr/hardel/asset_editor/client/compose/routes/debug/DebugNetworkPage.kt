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
import androidx.compose.foundation.layout.heightIn
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
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.Button
import fr.hardel.asset_editor.client.compose.components.ui.ButtonSize
import fr.hardel.asset_editor.client.compose.components.ui.ButtonVariant
import fr.hardel.asset_editor.client.compose.components.ui.CopyButton
import fr.hardel.asset_editor.client.compose.components.ui.datatable.DataTable
import fr.hardel.asset_editor.client.compose.components.ui.Dropdown
import fr.hardel.asset_editor.client.compose.components.ui.KeyValueGrid
import fr.hardel.asset_editor.client.compose.components.ui.datatable.TableColumn
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.asFlow
import fr.hardel.asset_editor.client.memory.session.debug.NetworkTraceMemory
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import net.minecraft.client.resources.language.I18n

private val NETWORK_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault())

@Composable
fun DebugNetworkPage(context: StudioContext) {
    var currentPage by remember { mutableStateOf(0) }
    var expandedIds by remember { mutableStateOf(emptySet<Long>()) }
    var selectedIds by remember { mutableStateOf(emptySet<Long>()) }
    val flow = remember(context) { context.debugMemory().network().asFlow() }
    val snapshot by flow.collectAsState(context.debugMemory().network().snapshot())
    val allLabel = I18n.get("generic:all")
    val selectedNamespace = snapshot.selectedNamespace ?: allLabel
    val entries = snapshot.entries
    val selectionMode = selectedIds.isNotEmpty()
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
        if (selectionMode) {
            SelectionActionBar(
                selectedCount = selectedIds.size,
                onCopySelected = {
                    val selectedEntries = entries.filter { selectedIds.contains(it.id()) }
                    copyEntriesToClipboard(selectedEntries)
                },
                onSelectAll = { selectedIds = entries.map { it.id() }.toSet() },
                onDeselectAll = { selectedIds = emptySet() },
                onDeleteSelected = {
                    context.debugMemory().network().removeByIds(selectedIds)
                    selectedIds = emptySet()
                }
            )
        } else {
            NormalActionBar(
                entryCount = entries.size,
                namespaces = namespaces,
                selectedNamespace = selectedNamespace,
                onNamespaceChange = { namespace ->
                    context.debugMemory().network().selectNamespace(if (namespace == allLabel) null else namespace)
                },
                onCopyAll = { copyEntriesToClipboard(entries) },
                onClear = context.debugMemory().network()::clear
            )
        }

        DataTable(
            items = entries,
            columns = listOf(
                TableColumn(I18n.get("debug:network.column.time"), weight = 0.8f) { entry ->
                    Text(NETWORK_TIME_FORMAT.format(Instant.ofEpochMilli(entry.timestamp())), style = StudioTypography.regular(11), color = StudioColors.Zinc500)
                },
                TableColumn(I18n.get("debug:network.column.direction"), weight = 0.7f) { entry ->
                    DirectionBadge(entry.direction())
                },
                TableColumn(I18n.get("debug:network.column.payload_id"), weight = 1.4f) { entry ->
                    Text(entry.payloadId().path, style = StudioTypography.medium(11), color = StudioColors.Zinc400)
                },
                TableColumn(I18n.get("debug:network.column.title"), weight = 1.4f) { entry ->
                    val titleKey = "debug:payload.${entry.payloadId().namespace}.${entry.payloadId().path.replace('/', '.')}.title"
                    Text(if (I18n.exists(titleKey)) I18n.get(titleKey) else entry.payloadId().path, style = StudioTypography.regular(13), color = StudioColors.Zinc300)
                },
                TableColumn(I18n.get("debug:network.column.description"), weight = 2.2f) { entry ->
                    val descKey = "debug:payload.${entry.payloadId().namespace}.${entry.payloadId().path.replace('/', '.')}.description"
                    Text(if (I18n.exists(descKey)) I18n.get(descKey) else "", style = StudioTypography.regular(11), color = StudioColors.Zinc500)
                },
                TableColumn("", weight = 0.35f) { entry ->
                    CopyButton(
                        iconSize = 14.dp,
                        textProvider = { serializeEntry(entry) }
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
            selectedIds = selectedIds,
            onSelectionChange = { newSelection -> selectedIds = newSelection },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )
    }
}

@Composable
private fun NormalActionBar(
    entryCount: Int,
    namespaces: List<String>,
    selectedNamespace: String,
    onNamespaceChange: (String) -> Unit,
    onCopyAll: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp)
    ) {
        Text(
            text = I18n.get("debug:network.count", entryCount),
            style = StudioTypography.regular(12),
            color = StudioColors.Zinc500
        )
        Dropdown(
            items = namespaces,
            selected = selectedNamespace,
            labelExtractor = { value -> value },
            onSelect = onNamespaceChange
        )
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onCopyAll,
            variant = ButtonVariant.GHOST_BORDER,
            size = ButtonSize.SM,
            text = I18n.get("debug:action.copy_all")
        )
        Button(
            onClick = onClear,
            variant = ButtonVariant.GHOST_BORDER,
            size = ButtonSize.SM,
            text = I18n.get("debug:action.clear")
        )
    }
}

@Composable
private fun SelectionActionBar(
    selectedCount: Int,
    onCopySelected: () -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp)
    ) {
        Text(
            text = I18n.get("debug:network.selected", selectedCount),
            style = StudioTypography.medium(12),
            color = StudioColors.Zinc300
        )
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onCopySelected,
            variant = ButtonVariant.GHOST_BORDER,
            size = ButtonSize.SM,
            text = I18n.get("debug:action.copy_selected")
        )
        Button(
            onClick = onSelectAll,
            variant = ButtonVariant.GHOST_BORDER,
            size = ButtonSize.SM,
            text = I18n.get("debug:action.select_all")
        )
        Button(
            onClick = onDeselectAll,
            variant = ButtonVariant.GHOST_BORDER,
            size = ButtonSize.SM,
            text = I18n.get("debug:action.deselect_all")
        )
        Button(
            onClick = onDeleteSelected,
            variant = ButtonVariant.GHOST_BORDER,
            size = ButtonSize.SM,
            text = I18n.get("debug:action.delete_selected")
        )
    }
}

@Composable
private fun DirectionBadge(direction: NetworkTraceMemory.Direction) {
    val inbound = direction == NetworkTraceMemory.Direction.INBOUND

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = if (inbound) "S" else "C",
            style = StudioTypography.semiBold(11),
            color = if (inbound) StudioColors.Red400 else StudioColors.Emerald400
        )
        Text("->", style = StudioTypography.medium(11), color = StudioColors.Zinc600)
        Text(
            text = if (inbound) "C" else "S",
            style = StudioTypography.semiBold(11),
            color = if (inbound) StudioColors.Emerald400 else StudioColors.Red400
        )
    }
}

@Composable
private fun TraceExpandContent(entry: NetworkTraceMemory.TraceEntry) {
    val inbound = entry.direction() == NetworkTraceMemory.Direction.INBOUND
    val directionLabel = if (inbound) "Server -> Client" else "Client -> Server"
    val directionColor = if (inbound) StudioColors.Red400 else StudioColors.Emerald400

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
                text = NETWORK_TIME_FORMAT.format(Instant.ofEpochMilli(entry.timestamp())),
                style = StudioTypography.regular(11),
                color = StudioColors.Zinc500
            )
            CopyButton(
                iconSize = 14.dp,
                textProvider = { serializeEntry(entry) }
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

private fun copyEntriesToClipboard(entries: List<NetworkTraceMemory.TraceEntry>) {
    val json = entries.joinToString(",\n  ", "[\n  ", "\n]") { serializeEntry(it) }
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    clipboard.setContents(StringSelection(json), null)
}

private fun serializeEntry(entry: NetworkTraceMemory.TraceEntry): String {
    val payloadJson = serializePayload(entry.payload())
    return buildString {
        append("{\"id\":").append(entry.id())
        append(",\"timestamp\":").append(entry.timestamp())
        append(",\"direction\":\"").append(entry.direction()).append('"')
        append(",\"payloadId\":\"").append(entry.payloadId()).append('"')
        if (payloadJson != null) {
            append(",\"payload\":").append(payloadJson)
        }
        append('}')
    }
}

private fun serializePayload(payload: Any?): String? {
    if (payload == null || !payload.javaClass.isRecord) return null
    val components = payload.javaClass.recordComponents ?: return null
    if (components.isEmpty()) return null

    val fields = components.mapNotNull { component ->
        val value = component.accessor.invoke(payload)
        if (value is Collection<*> || (value != null && value.javaClass.isArray)) return@mapNotNull null
        "\"${component.name}\":${serializeValue(value)}"
    }.joinToString(",")
    return "{$fields}"
}

private fun serializeValue(value: Any?): String = when {
    value == null -> "null"
    value is String -> "\"${value.replace("\"", "\\\"")}\""
    value is Number || value is Boolean -> value.toString()
    value.javaClass.isRecord -> serializePayload(value) ?: "\"${value}\""
    value.javaClass.isEnum -> "\"${value}\""
    else -> "\"${value}\""
}

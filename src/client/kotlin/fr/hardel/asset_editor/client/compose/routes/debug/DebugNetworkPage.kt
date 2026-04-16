package fr.hardel.asset_editor.client.compose.routes.debug

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.page.debug.DEBUG_TIME_FORMAT
import fr.hardel.asset_editor.client.compose.components.page.debug.DebugNetworkDirectionBadge
import fr.hardel.asset_editor.client.compose.components.page.debug.DebugNetworkNormalActionBar
import fr.hardel.asset_editor.client.compose.components.page.debug.DebugNetworkSelectionActionBar
import fr.hardel.asset_editor.client.compose.components.page.debug.DebugNetworkTraceExpand
import fr.hardel.asset_editor.client.compose.components.page.debug.copyNetworkEntriesToClipboard
import fr.hardel.asset_editor.client.compose.components.page.debug.serializeNetworkEntry
import fr.hardel.asset_editor.client.compose.components.ui.CopyButton
import fr.hardel.asset_editor.client.compose.components.ui.datatable.DataTable
import fr.hardel.asset_editor.client.compose.components.ui.datatable.TableColumn
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.asFlow
import net.minecraft.client.resources.language.I18n
import java.time.Instant

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
            DebugNetworkSelectionActionBar(
                selectedCount = selectedIds.size,
                onCopySelected = {
                    val selectedEntries = entries.filter { selectedIds.contains(it.id()) }
                    copyNetworkEntriesToClipboard(selectedEntries)
                },
                onSelectAll = { selectedIds = entries.map { it.id() }.toSet() },
                onDeselectAll = { selectedIds = emptySet() },
                onDeleteSelected = {
                    context.debugMemory().network().removeByIds(selectedIds)
                    selectedIds = emptySet()
                }
            )
        } else {
            DebugNetworkNormalActionBar(
                entryCount = entries.size,
                namespaces = namespaces,
                selectedNamespace = selectedNamespace,
                onNamespaceChange = { namespace ->
                    context.debugMemory().network().selectNamespace(if (namespace == allLabel) null else namespace)
                },
                onCopyAll = { copyNetworkEntriesToClipboard(entries) },
                onClear = context.debugMemory().network()::clear
            )
        }

        DataTable(
            items = entries,
            columns = listOf(
                TableColumn(I18n.get("debug:network.column.time"), weight = 0.8f) { entry ->
                    Text(
                        DEBUG_TIME_FORMAT.format(Instant.ofEpochMilli(entry.timestamp())),
                        style = StudioTypography.regular(11),
                        color = StudioColors.Zinc500
                    )
                },
                TableColumn(I18n.get("debug:network.column.direction"), weight = 0.7f) { entry ->
                    DebugNetworkDirectionBadge(entry.direction())
                },
                TableColumn(I18n.get("debug:network.column.payload_id"), weight = 1.4f) { entry ->
                    Text(entry.payloadId().path, style = StudioTypography.medium(11), color = StudioColors.Zinc400)
                },
                TableColumn(I18n.get("debug:network.column.title"), weight = 1.4f) { entry ->
                    val titleKey = "debug:payload.${entry.payloadId().namespace}.${entry.payloadId().path.replace('/', '.')}.title"
                    Text(
                        if (I18n.exists(titleKey)) I18n.get(titleKey) else entry.payloadId().path,
                        style = StudioTypography.regular(13),
                        color = StudioColors.Zinc300
                    )
                },
                TableColumn(I18n.get("debug:network.column.description"), weight = 2.2f) { entry ->
                    val descKey = "debug:payload.${entry.payloadId().namespace}.${entry.payloadId().path.replace('/', '.')}.description"
                    Text(
                        if (I18n.exists(descKey)) I18n.get(descKey) else "",
                        style = StudioTypography.regular(11),
                        color = StudioColors.Zinc500
                    )
                },
                TableColumn("", weight = 0.35f) { entry ->
                    CopyButton(iconSize = 14.dp, textProvider = { serializeNetworkEntry(entry) })
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
            expandContent = { entry -> DebugNetworkTraceExpand(entry) },
            selectedIds = selectedIds,
            onSelectionChange = { newSelection -> selectedIds = newSelection },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )
    }
}

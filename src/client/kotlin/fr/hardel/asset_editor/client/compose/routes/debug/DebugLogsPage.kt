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
import fr.hardel.asset_editor.client.compose.components.page.debug.DebugLogsActionBar
import fr.hardel.asset_editor.client.compose.components.page.debug.DebugLogsExpandRow
import fr.hardel.asset_editor.client.compose.components.page.debug.copyDebugLogsToClipboard
import fr.hardel.asset_editor.client.compose.components.page.debug.debugLogLevelColor
import fr.hardel.asset_editor.client.compose.components.ui.datatable.DataTable
import fr.hardel.asset_editor.client.compose.components.ui.datatable.TableColumn
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.asFlow
import net.minecraft.client.resources.language.I18n
import java.time.Instant

@Composable
fun DebugLogsPage(context: StudioContext) {
    var currentPage by remember { mutableStateOf(0) }
    var expandedIds by remember { mutableStateOf(emptySet<Long>()) }
    val flow = remember(context) { context.debugMemory().logs().asFlow() }
    val snapshot by flow.collectAsState(context.debugMemory().logs().snapshot())
    val entries = snapshot.entries

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        DebugLogsActionBar(
            entryCount = entries.size,
            onCopyAll = { copyDebugLogsToClipboard(entries) },
            onClear = context.debugMemory().logs()::clear
        )

        DataTable(
            items = entries,
            columns = listOf(
                TableColumn(I18n.get("debug:logs.column.time"), weight = 0.8f) { entry ->
                    Text(
                        DEBUG_TIME_FORMAT.format(Instant.ofEpochMilli(entry.timestamp())),
                        style = StudioTypography.regular(11),
                        color = StudioColors.Zinc500
                    )
                },
                TableColumn(I18n.get("debug:logs.column.level"), weight = 0.7f) { entry ->
                    Text(
                        I18n.get("debug:logs.level.${entry.level().name.lowercase()}"),
                        style = StudioTypography.semiBold(11),
                        color = debugLogLevelColor(entry.level())
                    )
                },
                TableColumn(I18n.get("debug:logs.column.category"), weight = 0.9f) { entry ->
                    Text(
                        I18n.get("debug:logs.category.${entry.category().name.lowercase()}"),
                        style = StudioTypography.medium(11),
                        color = StudioColors.Zinc400
                    )
                },
                TableColumn(I18n.get("debug:logs.column.message"), weight = 2.6f) { entry ->
                    Text(entry.message(), style = StudioTypography.regular(13), color = StudioColors.Zinc300)
                }
            ),
            pageSize = 50,
            currentPage = currentPage,
            onPageChange = { page -> currentPage = page },
            placeholder = I18n.get("debug:logs.placeholder"),
            idExtractor = { entry -> entry.id() },
            expandedIds = expandedIds,
            onToggleExpand = { id -> expandedIds = if (id in expandedIds) expandedIds - id else expandedIds + id },
            expandContent = { entry -> DebugLogsExpandRow(entry) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )
    }
}

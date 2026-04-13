package fr.hardel.asset_editor.client.compose.routes.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.Button
import fr.hardel.asset_editor.client.compose.components.ui.ButtonSize
import fr.hardel.asset_editor.client.compose.components.ui.ButtonVariant
import fr.hardel.asset_editor.client.compose.components.ui.CopyButton
import fr.hardel.asset_editor.client.compose.components.ui.datatable.DataTable
import fr.hardel.asset_editor.client.compose.components.ui.datatable.TableColumn
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.asFlow
import fr.hardel.asset_editor.client.memory.session.debug.DebugLogMemory
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import net.minecraft.client.resources.language.I18n

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault())

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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = I18n.get("debug:logs.count", entries.size),
                style = StudioTypography.regular(12),
                color = StudioColors.Zinc500
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    val serialized = buildString {
                        append("[\n")
                        entries.forEachIndexed { index, entry ->
                            append("  {\"timestamp\":${entry.timestamp()},\"level\":\"${entry.level()}\",\"category\":\"${entry.category()}\",\"message\":")
                            append("\"${entry.message().replace("\"", "\\\"")}\"")
                            if (entry.data().isNotEmpty()) {
                                append(",\"data\":{")
                                entry.data().entries.joinToString(",") { "\"${it.key}\":\"${it.value.replace("\"", "\\\"")}\"" }.also(::append)
                                append("}")
                            }
                            append("}")
                            if (index < entries.lastIndex) append(",")
                            append("\n")
                        }
                        append("]")
                    }
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(serialized), null)
                },
                variant = ButtonVariant.GHOST_BORDER,
                size = ButtonSize.SM,
                text = I18n.get("debug:logs.action.copy_all")
            )
            Button(
                onClick = context.debugMemory().logs()::clear,
                variant = ButtonVariant.GHOST_BORDER,
                size = ButtonSize.SM,
                text = I18n.get("debug:action.clear")
            )
        }

        DataTable(
            items = entries,
            columns = listOf(
                TableColumn(I18n.get("debug:logs.column.time"), weight = 0.8f) { entry ->
                    Text(TIME_FORMAT.format(Instant.ofEpochMilli(entry.timestamp())), style = StudioTypography.regular(11), color = StudioColors.Zinc500)
                },
                TableColumn(I18n.get("debug:logs.column.level"), weight = 0.7f) { entry ->
                    Text(I18n.get("debug:logs.level.${entry.level().name.lowercase()}"), style = StudioTypography.semiBold(11), color = levelColor(entry.level()))
                },
                TableColumn(I18n.get("debug:logs.column.category"), weight = 0.9f) { entry ->
                    Text(I18n.get("debug:logs.category.${entry.category().name.lowercase()}"), style = StudioTypography.medium(11), color = StudioColors.Zinc400)
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
            expandContent = { entry ->
                if (entry.data().isEmpty()) {
                    Text(
                        text = I18n.get("debug:logs.no_additional_data"),
                        style = StudioTypography.regular(12),
                        color = StudioColors.Zinc500
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        entry.data().forEach { (key, value) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(key, style = StudioTypography.medium(11), color = StudioColors.Zinc500)
                                Text(value, style = StudioTypography.regular(12), color = StudioColors.Zinc300, modifier = Modifier.weight(1f))
                                CopyButton(textProvider = { value })
                            }
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )
    }
}

private fun levelColor(level: DebugLogMemory.Level): Color =
    when (level) {
        DebugLogMemory.Level.INFO -> StudioColors.Sky400
        DebugLogMemory.Level.WARN -> StudioColors.Amber400
        DebugLogMemory.Level.ERROR -> StudioColors.Red400
        DebugLogMemory.Level.SUCCESS -> StudioColors.Zinc100
    }

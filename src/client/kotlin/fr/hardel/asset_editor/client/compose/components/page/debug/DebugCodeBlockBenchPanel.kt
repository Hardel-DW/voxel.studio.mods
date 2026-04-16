package fr.hardel.asset_editor.client.compose.components.page.debug

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.datatable.DataTable
import fr.hardel.asset_editor.client.compose.components.ui.datatable.TableColumn
import net.minecraft.client.resources.language.I18n

@Composable
fun DebugCodeBlockBenchPanel(title: String, entries: List<BenchEntry>) {
    val rows = remember(entries) { entries }
    DataTable(
        items = rows,
        columns = listOf(
            TableColumn(header = I18n.get("debug:code.bench.column.stage", title), weight = 2f) { entry ->
                Text(
                    text = entry.stage,
                    style = StudioTypography.regular(11).copy(fontFamily = FontFamily.Monospace),
                    color = StudioColors.Zinc400
                )
            },
            TableColumn(header = I18n.get("debug:code.bench.column.ms"), weight = 1f) { entry ->
                Text(
                    text = I18n.get("debug:code.bench.value", entry.millis),
                    style = StudioTypography.regular(11).copy(fontFamily = FontFamily.Monospace),
                    color = if (entry.millis > 50) StudioColors.Zinc200 else StudioColors.Zinc500
                )
            }
        ),
        modifier = Modifier.fillMaxWidth().height(140.dp)
    )
}

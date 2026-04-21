package fr.hardel.asset_editor.client.compose.components.page.debug.workspace.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.page.debug.DebugWorkspaceEmptyState
import fr.hardel.asset_editor.client.compose.components.page.debug.DebugWorkspaceHeader
import fr.hardel.asset_editor.client.compose.components.ui.datatable.DataTable
import fr.hardel.asset_editor.client.compose.components.ui.datatable.TableColumn
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.rememberUiSnapshot
import fr.hardel.asset_editor.client.memory.session.ui.ConceptUiSnapshot
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val EXPAND_SHAPE = RoundedCornerShape(8.dp)

private data class ConceptRow(
    val conceptId: Identifier,
    val snapshot: ConceptUiSnapshot
)

@Composable
fun DebugWorkspaceUiStatePanel(
    context: StudioContext,
    modifier: Modifier = Modifier
) {
    val uiSnapshot = rememberUiSnapshot(context)
    val rows = remember(uiSnapshot) {
        uiSnapshot.concepts.entries.map { ConceptRow(it.key, it.value) }
    }
    var expandedIds by remember { mutableStateOf(emptySet<Long>()) }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        DebugWorkspaceHeader(
            title = I18n.get("debug:workspace.panel.ui_state.title"),
            subtitle = I18n.get("debug:workspace.panel.ui_state.subtitle", rows.size)
        )

        if (rows.isEmpty()) {
            DebugWorkspaceEmptyState(
                title = I18n.get("debug:workspace.ui_state.empty.title"),
                subtitle = I18n.get("debug:workspace.ui_state.empty.subtitle")
            )
            return@Column
        }

        DataTable(
            items = rows,
            lazy = true,
            columns = listOf(
                TableColumn(I18n.get("debug:workspace.ui_state.column.concept"), weight = 1.2f) { row ->
                    Text(
                        row.conceptId.toString(),
                        style = StudioTypography.medium(12),
                        color = StudioColors.Zinc200,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                TableColumn(I18n.get("debug:workspace.ui_state.column.search"), weight = 1f) { row ->
                    Text(
                        row.snapshot.search().ifBlank { I18n.get("debug:workspace.none") },
                        style = StudioTypography.regular(11),
                        color = if (row.snapshot.search().isBlank()) StudioColors.Zinc600
                        else StudioColors.Zinc300,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                TableColumn(I18n.get("debug:workspace.ui_state.column.view"), weight = 0.8f) { row ->
                    Text(
                        row.snapshot.sidebarView().name,
                        style = StudioTypography.medium(11),
                        color = StudioColors.Zinc100
                    )
                },
                TableColumn(I18n.get("debug:workspace.ui_state.column.show_all"), weight = 0.5f) { row ->
                    Text(
                        if (row.snapshot.showAll())
                            I18n.get("debug:workspace.bool.true")
                        else
                            I18n.get("debug:workspace.bool.false"),
                        style = StudioTypography.regular(11),
                        color = if (row.snapshot.showAll()) StudioColors.Emerald400 else StudioColors.Zinc500
                    )
                },
                TableColumn(I18n.get("debug:workspace.ui_state.column.tree"), weight = 0.6f) { row ->
                    Text(
                        row.snapshot.treeExpansion().size.toString(),
                        style = StudioTypography.medium(11),
                        color = StudioColors.Zinc400
                    )
                }
            ),
            idExtractor = { it.conceptId.hashCode().toLong() },
            expandedIds = expandedIds,
            onToggleExpand = { id ->
                expandedIds = if (id in expandedIds) expandedIds - id else expandedIds + id
            },
            expandContent = { row -> ConceptDetail(row) },
            placeholder = I18n.get("debug:workspace.ui_state.empty.title"),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ConceptDetail(row: ConceptRow) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(EXPAND_SHAPE)
            .background(StudioColors.Zinc950.copy(alpha = 0.4f))
            .border(1.dp, StudioColors.Zinc800.copy(alpha = 0.5f), EXPAND_SHAPE)
            .padding(16.dp)
    ) {
        DetailLine(I18n.get("debug:workspace.ui_state.search"), row.snapshot.search())
        DetailLine(I18n.get("debug:workspace.ui_state.filter_path"), row.snapshot.filterPath())
        DetailLine(I18n.get("debug:workspace.ui_state.sidebar_view"), row.snapshot.sidebarView().name)
        DetailLine(
            I18n.get("debug:workspace.ui_state.show_all"),
            if (row.snapshot.showAll()) I18n.get("debug:workspace.bool.true")
            else I18n.get("debug:workspace.bool.false")
        )

        if (row.snapshot.treeExpansion().isNotEmpty()) {
            Text(
                text = I18n.get("debug:workspace.ui_state.tree_expansion").uppercase(),
                style = StudioTypography.medium(10),
                color = StudioColors.Zinc500,
                modifier = Modifier.padding(top = 4.dp)
            )
            row.snapshot.treeExpansion().entries.sortedBy { it.key }.forEach { (path, expanded) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        path,
                        style = StudioTypography.regular(11),
                        color = StudioColors.Zinc400,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (expanded) "▾" else "▸",
                        style = StudioTypography.medium(12),
                        color = if (expanded) StudioColors.Emerald400 else StudioColors.Zinc500
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            label.uppercase(),
            style = StudioTypography.medium(10),
            color = StudioColors.Zinc500,
            modifier = Modifier.padding(end = 4.dp)
        )
        Text(
            value.ifBlank { I18n.get("debug:workspace.none") },
            style = StudioTypography.regular(12),
            color = if (value.isBlank()) StudioColors.Zinc600 else StudioColors.Zinc300,
            modifier = Modifier.weight(1f)
        )
    }
}

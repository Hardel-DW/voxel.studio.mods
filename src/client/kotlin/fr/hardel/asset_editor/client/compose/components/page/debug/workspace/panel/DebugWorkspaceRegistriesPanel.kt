package fr.hardel.asset_editor.client.compose.components.page.debug.workspace.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import fr.hardel.asset_editor.client.compose.components.ui.InputText
import fr.hardel.asset_editor.client.compose.components.ui.datatable.DataTable
import fr.hardel.asset_editor.client.compose.components.ui.datatable.TableColumn
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.rememberMemoryValue
import fr.hardel.asset_editor.workspace.flush.ElementEntry
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val EXPAND_SHAPE = RoundedCornerShape(8.dp)
private val CHIP_SHAPE = RoundedCornerShape(5.dp)
private const val ELEMENTS_VISIBLE = 40

private data class RegistryRow(
    val registryId: String,
    val entries: List<ElementEntry<*>>
)

@Composable
fun DebugWorkspaceRegistriesPanel(
    context: StudioContext,
    modifier: Modifier = Modifier
) {
    val registries = rememberMemoryValue(context.registryMemory()) { it.registries }
    var search by remember { mutableStateOf("") }
    var expandedIds by remember { mutableStateOf(emptySet<Long>()) }

    val rows = remember(registries, search) {
        val needle = search.trim().lowercase()
        registries.entries
            .map { (key, value) -> RegistryRow(key, value.values.toList()) }
            .filter { row ->
                if (needle.isEmpty()) return@filter true
                row.registryId.lowercase().contains(needle) ||
                    row.entries.any { entry -> entry.id().toString().lowercase().contains(needle) }
            }
            .sortedBy { it.registryId }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        RegistriesHeader(
            total = registries.size,
            search = search,
            onSearchChange = { search = it }
        )

        if (rows.isEmpty()) {
            DebugWorkspaceEmptyState(
                title = I18n.get("debug:workspace.registries.empty.title"),
                subtitle = I18n.get("debug:workspace.registries.empty.subtitle")
            )
            return@Column
        }

        DataTable(
            items = rows,
            lazy = true,
            columns = listOf(
                TableColumn(I18n.get("debug:workspace.registries.column.registry"), weight = 2.5f) { row ->
                    Text(
                        row.registryId,
                        style = StudioTypography.medium(12),
                        color = StudioColors.Zinc200,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                TableColumn(I18n.get("debug:workspace.registries.column.count"), weight = 0.7f) { row ->
                    Text(
                        row.entries.size.toString(),
                        style = StudioTypography.semiBold(12),
                        color = StudioColors.Zinc100
                    )
                },
                TableColumn(I18n.get("debug:workspace.registries.column.tagged"), weight = 0.7f) { row ->
                    val tagged = row.entries.count { it.tags().isNotEmpty() }
                    Text(
                        tagged.toString(),
                        style = StudioTypography.regular(12),
                        color = StudioColors.Zinc400
                    )
                }
            ),
            idExtractor = { it.registryId.hashCode().toLong() },
            expandedIds = expandedIds,
            onToggleExpand = { id ->
                expandedIds = if (id in expandedIds) expandedIds - id else expandedIds + id
            },
            expandContent = { row -> ElementsPreview(row) },
            placeholder = I18n.get("debug:workspace.registries.empty.title"),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RegistriesHeader(
    total: Int,
    search: String,
    onSearchChange: (String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = I18n.get("debug:workspace.panel.registries.title"),
                style = StudioTypography.semiBold(20),
                color = StudioColors.Zinc100
            )
            Text(
                text = I18n.get("debug:workspace.panel.registries.subtitle", total),
                style = StudioTypography.regular(12),
                color = StudioColors.Zinc500
            )
        }
        InputText(
            value = search,
            onValueChange = onSearchChange,
            placeholder = I18n.get("debug:workspace.registries.search_placeholder"),
            focusExpand = false,
            maxWidth = 280.dp
        )
    }
}

@Composable
private fun ElementsPreview(row: RegistryRow) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(EXPAND_SHAPE)
            .background(StudioColors.Zinc950.copy(alpha = 0.4f))
            .border(1.dp, StudioColors.Zinc800.copy(alpha = 0.5f), EXPAND_SHAPE)
            .padding(12.dp)
            .heightIn(max = 420.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = I18n.get("debug:workspace.registries.elements_label").uppercase(),
                style = StudioTypography.medium(10),
                color = StudioColors.Zinc500
            )
            Text(
                text = I18n.get("debug:workspace.registries.elements_count", row.entries.size),
                style = StudioTypography.regular(10),
                color = StudioColors.Zinc600
            )
        }

        val visible = row.entries.take(ELEMENTS_VISIBLE)
        visible.forEach { entry -> ElementLine(entry) }

        val remaining = row.entries.size - visible.size
        if (remaining > 0) {
            Text(
                text = I18n.get("debug:workspace.registries.more_elements", remaining),
                style = StudioTypography.regular(11),
                color = StudioColors.Zinc600,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun ElementLine(entry: ElementEntry<*>) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = entry.id().toString(),
            style = StudioTypography.regular(11),
            color = StudioColors.Zinc300,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (entry.tags().isNotEmpty()) {
            TagCountChip(entry.tags())
        }
    }
}

@Composable
private fun TagCountChip(tags: Set<Identifier>) {
    Box(
        modifier = Modifier
            .clip(CHIP_SHAPE)
            .background(StudioColors.Zinc800.copy(alpha = 0.5f))
            .border(1.dp, StudioColors.Zinc700.copy(alpha = 0.4f), CHIP_SHAPE)
            .padding(horizontal = 6.dp, vertical = 1.dp)
    ) {
        Text(
            text = I18n.get("debug:workspace.registries.tag_count", tags.size),
            style = StudioTypography.medium(10),
            color = StudioColors.Zinc300
        )
    }
}

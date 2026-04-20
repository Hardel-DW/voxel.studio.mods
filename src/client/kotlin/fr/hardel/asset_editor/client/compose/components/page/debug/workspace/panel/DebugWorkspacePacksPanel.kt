package fr.hardel.asset_editor.client.compose.components.page.debug.workspace.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import fr.hardel.asset_editor.client.compose.lib.rememberAvailablePacks
import fr.hardel.asset_editor.client.compose.lib.rememberSelectedPack
import fr.hardel.asset_editor.client.memory.session.ui.ClientPackInfo
import net.minecraft.client.resources.language.I18n

private val NAMESPACE_CHIP_SHAPE = RoundedCornerShape(6.dp)

@Composable
fun DebugWorkspacePacksPanel(
    context: StudioContext,
    modifier: Modifier = Modifier
) {
    val packs = rememberAvailablePacks(context)
    val selected = rememberSelectedPack(context)
    var expandedIds by remember { mutableStateOf(emptySet<Long>()) }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        DebugWorkspaceHeader(
            title = I18n.get("debug:workspace.panel.packs.title"),
            subtitle = I18n.get("debug:workspace.panel.packs.subtitle", packs.size)
        )

        if (packs.isEmpty()) {
            DebugWorkspaceEmptyState(
                title = I18n.get("debug:workspace.packs.empty.title"),
                subtitle = I18n.get("debug:workspace.packs.empty.subtitle")
            )
            return@Column
        }

        DataTable(
            items = packs,
            lazy = true,
            columns = listOf(
                TableColumn(header = "", weight = 0.3f) { pack ->
                    val active = pack.packId() == selected?.packId()
                    StatusDot(active)
                },
                TableColumn(I18n.get("debug:workspace.packs.column.id"), weight = 1.5f) { pack ->
                    Text(
                        pack.packId(),
                        style = StudioTypography.medium(12),
                        color = if (pack.packId() == selected?.packId())
                            StudioColors.Zinc100 else StudioColors.Zinc300,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                TableColumn(I18n.get("debug:workspace.packs.column.name"), weight = 2f) { pack ->
                    Text(
                        pack.name(),
                        style = StudioTypography.regular(12),
                        color = StudioColors.Zinc400,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                TableColumn(I18n.get("debug:workspace.packs.column.writable"), weight = 0.8f) { pack ->
                    WritableChip(writable = pack.writable())
                },
                TableColumn(I18n.get("debug:workspace.packs.column.namespaces"), weight = 0.7f) { pack ->
                    Text(
                        pack.namespaces().size.toString(),
                        style = StudioTypography.medium(12),
                        color = StudioColors.Zinc400
                    )
                }
            ),
            idExtractor = { it.packId().hashCode().toLong() },
            expandedIds = expandedIds,
            onToggleExpand = { id ->
                expandedIds = if (id in expandedIds) expandedIds - id else expandedIds + id
            },
            expandContent = { pack -> ExpandedPack(pack) },
            placeholder = I18n.get("debug:workspace.packs.empty.title"),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatusDot(active: Boolean) {
    val color = if (active) StudioColors.Zinc100 else StudioColors.Zinc700
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color)
    )
}

@Composable
private fun WritableChip(writable: Boolean) {
    val color = if (writable) StudioColors.Emerald400 else StudioColors.Zinc500
    Box(
        modifier = Modifier
            .clip(NAMESPACE_CHIP_SHAPE)
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.35f), NAMESPACE_CHIP_SHAPE)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = I18n.get(
                if (writable) "debug:workspace.packs.writable.true"
                else "debug:workspace.packs.writable.false"
            ),
            style = StudioTypography.semiBold(10),
            color = color
        )
    }
}

@Composable
private fun ExpandedPack(pack: ClientPackInfo) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(StudioColors.Zinc950.copy(alpha = 0.4f))
            .border(1.dp, StudioColors.Zinc800.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Text(
            text = I18n.get("debug:workspace.packs.namespaces_label").uppercase(),
            style = StudioTypography.medium(10),
            color = StudioColors.Zinc500
        )
        if (pack.namespaces().isEmpty()) {
            Text(
                text = I18n.get("debug:workspace.packs.namespaces.empty"),
                style = StudioTypography.regular(11),
                color = StudioColors.Zinc600
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                pack.namespaces().forEach { ns -> NamespaceChip(ns) }
            }
        }
    }
}

@Composable
private fun NamespaceChip(namespace: String) {
    Box(
        modifier = Modifier
            .clip(NAMESPACE_CHIP_SHAPE)
            .background(StudioColors.Zinc800.copy(alpha = 0.5f))
            .border(1.dp, StudioColors.Zinc700.copy(alpha = 0.5f), NAMESPACE_CHIP_SHAPE)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = namespace,
            style = StudioTypography.regular(11),
            color = StudioColors.Zinc300
        )
    }
}

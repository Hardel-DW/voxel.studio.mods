package fr.hardel.asset_editor.client.compose.components.page.debug.workspace.panel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.page.debug.DebugWorkspaceEmptyState
import fr.hardel.asset_editor.client.compose.components.page.debug.DebugWorkspaceHeader
import fr.hardel.asset_editor.client.compose.components.ui.CopyButton
import fr.hardel.asset_editor.client.compose.components.ui.datatable.DataTable
import fr.hardel.asset_editor.client.compose.components.ui.datatable.TableColumn
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.rememberMemoryValue
import net.minecraft.client.resources.language.I18n

@Composable
fun DebugWorkspacePendingPanel(
    context: StudioContext,
    modifier: Modifier = Modifier
) {
    val pending = rememberMemoryValue(context.gateway.pendingActionsMemory()) { it }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        DebugWorkspaceHeader(
            title = I18n.get("debug:workspace.panel.pending.title"),
            subtitle = I18n.get("debug:workspace.panel.pending.subtitle", pending.size)
        )

        if (pending.isEmpty()) {
            DebugWorkspaceEmptyState(
                title = I18n.get("debug:workspace.pending.empty.title"),
                subtitle = I18n.get("debug:workspace.pending.empty.subtitle")
            )
            return@Column
        }

        DataTable(
            items = pending,
            lazy = true,
            columns = listOf(
                TableColumn(I18n.get("debug:workspace.pending.column.action_id"), weight = 2f) { view ->
                    Text(
                        view.actionId.toString(),
                        style = StudioTypography.regular(11),
                        color = StudioColors.Zinc400,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                TableColumn(I18n.get("debug:workspace.pending.column.type"), weight = 1.2f) { view ->
                    Text(
                        view.actionType,
                        style = StudioTypography.medium(12),
                        color = StudioColors.Violet500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                TableColumn(I18n.get("debug:workspace.pending.column.pack"), weight = 1f) { view ->
                    Text(
                        view.packId,
                        style = StudioTypography.regular(11),
                        color = StudioColors.Zinc400,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                TableColumn(I18n.get("debug:workspace.pending.column.registry"), weight = 1.3f) { view ->
                    Text(
                        view.registryId.toString(),
                        style = StudioTypography.regular(11),
                        color = StudioColors.Zinc400,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                TableColumn(I18n.get("debug:workspace.pending.column.target"), weight = 1.5f) { view ->
                    Text(
                        view.target.toString(),
                        style = StudioTypography.regular(11),
                        color = StudioColors.Zinc300,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                TableColumn("", weight = 0.4f) { view ->
                    CopyButton(iconSize = 13.dp, textProvider = { serializePending(view) })
                }
            ),
            idExtractor = { it.actionId.hashCode().toLong() },
            placeholder = I18n.get("debug:workspace.pending.empty.title"),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
    }
}

private fun serializePending(view: Any): String = view.toString()

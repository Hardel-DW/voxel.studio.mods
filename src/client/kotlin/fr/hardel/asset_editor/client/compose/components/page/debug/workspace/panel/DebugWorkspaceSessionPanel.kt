package fr.hardel.asset_editor.client.compose.components.page.debug.workspace.panel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.page.debug.DebugWorkspaceHeader
import fr.hardel.asset_editor.client.compose.components.ui.CopyButton
import fr.hardel.asset_editor.client.compose.components.ui.datatable.DataTable
import fr.hardel.asset_editor.client.compose.components.ui.datatable.TableColumn
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.rememberMemoryValue
import net.minecraft.client.resources.language.I18n

private data class SessionVariable(val label: String, val value: String, val copyable: Boolean)

@Composable
fun DebugWorkspaceSessionPanel(
    context: StudioContext,
    modifier: Modifier = Modifier
) {
    val session = rememberMemoryValue(context.sessionMemory()) { it }
    val variables = listOf(
        SessionVariable(
            label = I18n.get("debug:workspace.session.is_admin"),
            value = formatBool(session.permissions.isAdmin),
            copyable = false
        ),
        SessionVariable(
            label = I18n.get("debug:workspace.session.permissions_received"),
            value = formatBool(session.permissionsReceived),
            copyable = false
        ),
        SessionVariable(
            label = I18n.get("debug:workspace.session.pack_list_received"),
            value = formatBool(session.packListReceived),
            copyable = false
        ),
        SessionVariable(
            label = I18n.get("debug:workspace.session.world_session_key"),
            value = session.worldSessionKey.ifBlank { I18n.get("debug:workspace.none") },
            copyable = session.worldSessionKey.isNotBlank()
        )
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        DebugWorkspaceHeader(
            title = I18n.get("debug:workspace.panel.session.title"),
            subtitle = I18n.get("debug:workspace.panel.session.subtitle")
        )

        DataTable(
            items = variables,
            columns = listOf(
                TableColumn(I18n.get("debug:workspace.session.column.variable"), weight = 1.1f) { variable ->
                    Text(
                        variable.label,
                        style = StudioTypography.regular(12),
                        color = StudioColors.Zinc400,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                TableColumn(I18n.get("debug:workspace.session.column.value"), weight = 2.4f) { variable ->
                    Text(
                        variable.value,
                        style = StudioTypography.regular(12),
                        color = StudioColors.Zinc200,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                TableColumn("", weight = 0.3f) { variable ->
                    if (variable.copyable) CopyButton(iconSize = 13.dp, textProvider = { variable.value })
                }
            ),
            idExtractor = { it.label.hashCode().toLong() },
            placeholder = ""
        )
    }
}

private fun formatBool(value: Boolean): String =
    if (value) I18n.get("debug:workspace.bool.true") else I18n.get("debug:workspace.bool.false")

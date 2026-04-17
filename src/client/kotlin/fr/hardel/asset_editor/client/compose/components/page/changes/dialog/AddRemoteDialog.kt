package fr.hardel.asset_editor.client.compose.components.page.changes.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.components.ui.Button
import fr.hardel.asset_editor.client.compose.components.ui.ButtonSize
import fr.hardel.asset_editor.client.compose.components.ui.ButtonVariant
import fr.hardel.asset_editor.client.compose.components.ui.CommandPalette
import fr.hardel.asset_editor.client.compose.components.ui.CommandPaletteGroup
import fr.hardel.asset_editor.client.compose.components.ui.CommandPaletteHint
import fr.hardel.asset_editor.client.compose.components.ui.CommandPaletteItem
import fr.hardel.asset_editor.client.compose.lib.git.GitSnapshot
import net.minecraft.client.resources.language.I18n

@Composable
fun AddRemoteDialog(
    visible: Boolean,
    snapshot: GitSnapshot,
    onDismiss: () -> Unit,
    onSubmit: (name: String, url: String) -> Unit
) {
    if (!visible) return

    var url by remember { mutableStateOf("") }

    val suggestedName = remember(snapshot.remotes) {
        if (snapshot.remotes.none { it.name == "origin" }) "origin" else nextFreeName(snapshot)
    }

    CommandPalette(
        visible = true,
        title = I18n.get("changes:remote.add.title"),
        value = url,
        onValueChange = { url = it },
        placeholder = I18n.get("changes:remote.add.placeholder"),
        onDismiss = onDismiss
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            CommandPaletteHint(I18n.get("changes:remote.add.hint"))
            if (snapshot.remotes.isNotEmpty()) {
                CommandPaletteGroup(heading = I18n.get("changes:remote.add.existing").uppercase()) {
                    for (remote in snapshot.remotes) {
                        CommandPaletteItem(
                            label = remote.name,
                            onClick = {},
                            description = remote.url,
                            enabled = false
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 8.dp, end = 4.dp)
            ) {
                CommandPaletteHint(
                    I18n.get("changes:remote.add.name_as").replace("{name}", suggestedName)
                )
                Button(
                    onClick = {
                        val trimmed = url.trim()
                        if (trimmed.isNotBlank()) {
                            onSubmit(suggestedName, trimmed)
                        }
                    },
                    variant = ButtonVariant.DEFAULT,
                    size = ButtonSize.SM,
                    enabled = url.isNotBlank(),
                    text = I18n.get("changes:remote.add.submit")
                )
            }
        }
    }
}

private fun nextFreeName(snapshot: GitSnapshot): String {
    val used = snapshot.remotes.map { it.name }.toSet()
    if ("upstream" !in used) return "upstream"
    var index = 2
    while ("remote$index" in used) index++
    return "remote$index"
}

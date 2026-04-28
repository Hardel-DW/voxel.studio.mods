package fr.hardel.asset_editor.client.compose.routes.data

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonParser
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.mcdoc.McdocEditor
import fr.hardel.asset_editor.client.compose.components.ui.Button
import fr.hardel.asset_editor.client.compose.components.ui.ButtonSize
import fr.hardel.asset_editor.client.compose.components.ui.ButtonVariant
import fr.hardel.asset_editor.client.compose.lib.RegistryPageDialogs
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.dispatchRegistryAction
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentRegistryEntry
import fr.hardel.asset_editor.client.compose.lib.rememberRegistryDialogState
import fr.hardel.asset_editor.client.memory.core.ClientWorkspaceRegistry
import fr.hardel.asset_editor.client.mcdoc.McdocService
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType
import fr.hardel.asset_editor.workspace.action.SetEntryDataAction
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

@Composable
fun <T : Any> RegistryDataEditorPage(
    context: StudioContext,
    workspace: ClientWorkspaceRegistry<T>
) {
    val dialogs = rememberRegistryDialogState()
    val entry = rememberCurrentRegistryEntry(context, workspace) ?: return
    val registryId = workspace.registryId()
    val rootType = remember(registryId) { rootTypeFor(registryId) }

    if (rootType == null) {
        EmptyCodecState(registryId)
        return
    }

    val registries = Minecraft.getInstance().connection?.registryAccess() ?: return
    val initialJson = remember(entry.id()) { workspace.definition().encode(entry, registries) }
    var jsonText by remember(entry.id()) { mutableStateOf(initialJson) }
    var dirty by remember(entry.id()) { mutableStateOf(false) }
    var localError by remember(entry.id()) { mutableStateOf<String?>(null) }

    val parsedJson = remember(jsonText) {
        runCatching { JsonParser.parseString(jsonText) }.getOrNull()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 32.dp, end = 32.dp, top = 24.dp, bottom = if (dirty) 96.dp else 24.dp)
        ) {
            McdocEditor(
                type = rootType,
                value = parsedJson,
                onValueChange = { newValue: JsonElement ->
                    jsonText = newValue.toString()
                    dirty = true
                    localError = null
                }
            )
        }

        localError?.let { ErrorBanner(it, modifier = Modifier.align(Alignment.TopCenter)) }

        if (dirty) {
            DirtyBar(
                onSave = {
                    if (parsedJson == null) {
                        localError = "studio:data.invalid_json"
                        return@DirtyBar
                    }
                    context.dispatchRegistryAction(workspace, entry.id(), SetEntryDataAction(jsonText), dialogs)
                    dirty = false
                    localError = null
                },
                onDiscard = {
                    jsonText = initialJson
                    dirty = false
                    localError = null
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    RegistryPageDialogs(context, dialogs)
}

private fun rootTypeFor(registryId: Identifier): McdocType? {
    val key = registryId.path
    val entry = McdocService.current().dispatch().resolve("minecraft:resource", key).orElse(null) ?: return null
    return entry.target()
}

@Composable
private fun EmptyCodecState(registryId: Identifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = I18n.get("studio:data.empty_codec").replace("{0}", registryId.toString()),
            style = StudioTypography.regular(13),
            color = StudioColors.Zinc500
        )
    }
}

@Composable
private fun ErrorBanner(messageKey: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(StudioColors.Red500.copy(alpha = 0.15f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = I18n.get(messageKey),
            style = StudioTypography.medium(12),
            color = StudioColors.Red500
        )
    }
}

@Composable
private fun DirtyBar(
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioColors.Zinc950.copy(alpha = 0.85f))
            .padding(horizontal = 32.dp, vertical = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = I18n.get("studio:data.dirty_indicator"),
                style = StudioTypography.regular(12),
                color = StudioColors.Zinc400
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onDiscard,
                    variant = ButtonVariant.GHOST_BORDER,
                    size = ButtonSize.SM,
                    text = I18n.get("studio:data.discard")
                )
                Button(
                    onClick = onSave,
                    variant = ButtonVariant.SHIMMER,
                    size = ButtonSize.SM,
                    text = I18n.get("studio:data.save")
                )
            }
        }
    }
}


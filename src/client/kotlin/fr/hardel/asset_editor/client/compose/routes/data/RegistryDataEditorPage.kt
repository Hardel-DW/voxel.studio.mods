package fr.hardel.asset_editor.client.compose.routes.data

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonParser
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.mcdoc.McdocRoot
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.McdocTokens
import fr.hardel.asset_editor.client.compose.components.ui.editor.CodeEditor
import fr.hardel.asset_editor.client.compose.components.ui.editor.CodeEditorState
import fr.hardel.asset_editor.client.compose.lib.RegistryPageDialogs
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.dispatchRegistryAction
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentRegistryEntry
import fr.hardel.asset_editor.client.compose.lib.rememberRegistryDialogState
import fr.hardel.asset_editor.client.memory.core.ClientWorkspaceRegistry
import fr.hardel.asset_editor.client.mcdoc.McdocService
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType
import fr.hardel.asset_editor.workspace.action.SetEntryDataAction
import kotlinx.coroutines.delay
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private const val SAVE_DEBOUNCE_MS = 400L
private const val CODE_PARSE_DEBOUNCE_MS = 200L
private val PRETTY_GSON = GsonBuilder().setPrettyPrinting().create()

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
    val initial = remember(entry.id()) {
        runCatching { JsonParser.parseString(workspace.definition().encode(entry, registries)) }
            .getOrElse { JsonNull.INSTANCE }
    }
    var current by remember(entry.id()) { mutableStateOf<JsonElement>(initial) }
    var lastSaved by remember(entry.id()) { mutableStateOf(initial) }
    val codeState = remember(entry.id()) { CodeEditorState(PRETTY_GSON.toJson(initial)) }

    LaunchedEffect(current, entry.id()) {
        if (current == lastSaved) return@LaunchedEffect
        delay(SAVE_DEBOUNCE_MS)
        if (current == lastSaved) return@LaunchedEffect
        context.dispatchRegistryAction(workspace, entry.id(), SetEntryDataAction(current.toString()), dialogs)
        lastSaved = current
    }

    LaunchedEffect(current) {
        val codeJson = runCatching { JsonParser.parseString(codeState.fullText()) }.getOrNull()
        if (codeJson == current) return@LaunchedEffect
        val pretty = PRETTY_GSON.toJson(current)
        if (codeState.fullText() != pretty) {
            codeState.selectAll()
            codeState.insert(pretty)
        }
    }

    LaunchedEffect(codeState.documentVersion) {
        delay(CODE_PARSE_DEBOUNCE_MS)
        val parsed = runCatching { JsonParser.parseString(codeState.fullText()) }.getOrNull() ?: return@LaunchedEffect
        if (parsed != current) current = parsed
    }

    Row(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(0.62f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 24.dp)
        ) {
            Column(modifier = Modifier.widthIn(max = McdocTokens.MaxContentWidth)) {
                McdocRoot(
                    type = rootType,
                    value = current,
                    onValueChange = { current = it }
                )
            }
        }

        Spacer(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(McdocTokens.Border)
        )

        Column(
            modifier = Modifier
                .weight(0.38f)
                .widthIn(min = 280.dp, max = 420.dp)
                .fillMaxHeight()
                .background(McdocTokens.PopupBg)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = I18n.get("mcdoc:code_panel.title"),
                    style = StudioTypography.medium(12),
                    color = McdocTokens.TextDimmed
                )
            }
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 1.dp)
                    .background(McdocTokens.Border)
            )
            CodeEditor(
                state = codeState,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        }
    }

    RegistryPageDialogs(context, dialogs)
}

private fun rootTypeFor(registryId: Identifier): McdocType? {
    val entry = McdocService.current().dispatch().resolve("minecraft:resource", registryId.path).orElse(null) ?: return null
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

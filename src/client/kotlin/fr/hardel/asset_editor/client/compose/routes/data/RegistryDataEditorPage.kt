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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.memory.core.ClientWorkspaceRegistry
import net.minecraft.core.HolderLookup
import fr.hardel.asset_editor.client.compose.components.mcdoc.McdocRoot
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.McdocTokens
import fr.hardel.asset_editor.client.compose.components.ui.GridBackground
import fr.hardel.asset_editor.client.compose.components.ui.editor.CodeEditor
import fr.hardel.asset_editor.client.compose.components.ui.editor.CodeEditorState
import fr.hardel.asset_editor.client.compose.lib.RegistryPageDialogs
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.dispatchRegistryAction
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentRegistryEntry
import fr.hardel.asset_editor.client.compose.lib.rememberRegistryDialogState
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
    val entry = rememberCurrentRegistryEntry(context, workspace)
    if (entry == null) {
        DataEditorEmptyState(
            title = I18n.get("studio:data.empty.no_entry.title"),
            subtitle = I18n.get("studio:data.empty.no_entry.subtitle")
        )
        return
    }

    val registryId = workspace.registryId()
    val rootType = remember(registryId) { rootTypeFor(registryId) }

    if (rootType == null) {
        DataEditorEmptyState(
            title = I18n.get("studio:data.empty.no_codec.title"),
            subtitle = I18n.get("studio:data.empty.no_codec.subtitle").replace("{0}", registryId.toString())
        )
        return
    }

    val registries = Minecraft.getInstance().connection?.registryAccess() ?: return
    val initial = remember(entry.id()) {
        runCatching { JsonParser.parseString(workspace.definition().encode(entry, registries)) }
            .getOrElse { JsonNull.INSTANCE }
    }
    var current by remember(entry.id()) { mutableStateOf<JsonElement>(initial) }
    var lastSaved by remember(entry.id()) { mutableStateOf(initial) }
    var validationError by remember(entry.id()) { mutableStateOf<String?>(null) }
    val codeState = remember(entry.id()) { CodeEditorState(PRETTY_GSON.toJson(initial)) }

    LaunchedEffect(current, entry.id()) {
        if (current == lastSaved) return@LaunchedEffect
        delay(SAVE_DEBOUNCE_MS)
        if (current == lastSaved) return@LaunchedEffect
        val error = validate(workspace, current, registries)
        validationError = error
        if (error != null) return@LaunchedEffect
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
                .weight(0.55f)
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
                .weight(0.45f)
                .widthIn(min = 360.dp, max = 640.dp)
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
                if (validationError != null) {
                    Text(
                        text = I18n.get("mcdoc:code_panel.invalid"),
                        style = StudioTypography.regular(11),
                        color = McdocTokens.Error
                    )
                }
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

private fun <T : Any> validate(
    workspace: ClientWorkspaceRegistry<T>,
    json: JsonElement,
    registries: HolderLookup.Provider
): String? {
    val ops = registries.createSerializationContext(JsonOps.INSTANCE)
    val result = workspace.definition().codec().parse(ops, json)
    return result.error().map { it.message() }.orElse(null)
}

@Composable
private fun DataEditorEmptyState(
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioColors.Zinc950)
    ) {
        GridBackground()
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 480.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = StudioTypography.semiBold(20),
                color = StudioColors.Zinc100,
                textAlign = TextAlign.Start
            )
            Text(
                text = subtitle,
                style = StudioTypography.regular(13),
                color = StudioColors.Zinc400,
                textAlign = TextAlign.Start
            )
        }
    }
}

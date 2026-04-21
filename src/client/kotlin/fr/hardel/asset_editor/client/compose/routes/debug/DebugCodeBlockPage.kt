package fr.hardel.asset_editor.client.compose.routes.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.components.page.debug.DebugSidebar
import fr.hardel.asset_editor.client.compose.components.page.debug.DebugSidebarEntry
import fr.hardel.asset_editor.client.compose.components.page.debug.code.DEBUG_CODE_MONO_STYLE
import fr.hardel.asset_editor.client.compose.components.page.debug.code.DebugCodeBenchPanel
import fr.hardel.asset_editor.client.compose.components.page.debug.code.DebugCodePreparedSample
import fr.hardel.asset_editor.client.compose.components.page.debug.code.prepareDebugCodeSample
import fr.hardel.asset_editor.client.compose.components.page.debug.DebugWorkspaceHeader
import fr.hardel.asset_editor.client.compose.components.ui.CopyButton
import fr.hardel.asset_editor.client.compose.components.ui.InputText
import fr.hardel.asset_editor.client.compose.components.ui.LoadingPlaceholder
import fr.hardel.asset_editor.client.compose.components.ui.codeblock.CodeBlock
import fr.hardel.asset_editor.client.compose.components.ui.codeblock.CodeDiff
import fr.hardel.asset_editor.client.compose.components.ui.codeblock.DiffStatus
import fr.hardel.asset_editor.client.compose.components.ui.editor.CodeEditor
import kotlinx.coroutines.delay
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private const val TAB_CODEBLOCK = "codeblock"
private const val TAB_DIFF = "diff"
private const val TAB_EDITOR = "editor"
private val SIDEBAR_WIDTH = 260.dp
private const val DEBOUNCE_MS = 1000L

@Composable
fun DebugCodeBlockPage() {
    var lineCountInput by remember { mutableStateOf("256") }
    var prepared by remember { mutableStateOf<DebugCodePreparedSample?>(null) }
    var isGenerating by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(TAB_CODEBLOCK) }
    var firstRun by remember { mutableStateOf(true) }

    LaunchedEffect(lineCountInput) {
        val n = lineCountInput.toIntOrNull()?.coerceAtLeast(1) ?: return@LaunchedEffect
        isGenerating = true
        if (!firstRun) delay(DEBOUNCE_MS)
        firstRun = false
        prepared = prepareDebugCodeSample(n)
        isGenerating = false
    }

    val entries = listOf(
        DebugSidebarEntry(
            id = TAB_CODEBLOCK,
            icon = icon("pencil"),
            label = I18n.get("debug:code.nav.codeblock.label"),
            description = I18n.get("debug:code.nav.codeblock.description")
        ),
        DebugSidebarEntry(
            id = TAB_DIFF,
            icon = icon("git-commit"),
            label = I18n.get("debug:code.nav.diff.label"),
            description = I18n.get("debug:code.nav.diff.description")
        ),
        DebugSidebarEntry(
            id = TAB_EDITOR,
            icon = icon("pencil"),
            label = I18n.get("debug:code.nav.editor.label"),
            description = I18n.get("debug:code.nav.editor.description")
        )
    )

    Row(modifier = Modifier.fillMaxSize()) {
        DebugSidebar(
            entries = entries,
            selectedId = selectedTab,
            onSelect = { selectedTab = it },
            sectionLabel = I18n.get("debug:code.nav.section"),
            modifier = Modifier.width(SIDEBAR_WIDTH).fillMaxHeight()
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(StudioColors.Zinc950)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp)
            ) {
                val (title, subtitle) = tabHeader(selectedTab)
                DebugWorkspaceHeader(
                    title = title,
                    subtitle = subtitle,
                    actions = {
                        Box(
                            modifier = Modifier.size(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(
                                    color = StudioColors.Zinc400,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        InputText(
                            value = lineCountInput,
                            onValueChange = { lineCountInput = it.filter { c -> c.isDigit() }.take(7) },
                            placeholder = I18n.get("debug:code.input.placeholder"),
                            showSearchIcon = false,
                            modifier = Modifier.widthIn(max = 140.dp)
                        )
                    }
                )

                when (selectedTab) {
                    TAB_CODEBLOCK -> CodeBlockSection(prepared)
                    TAB_DIFF -> CodeDiffSection(prepared)
                    TAB_EDITOR -> CodeEditorSection(prepared)
                }
            }
        }
    }
}

@Composable
private fun CodeBlockSection(prepared: DebugCodePreparedSample?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
        if (prepared != null) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                CodeBlock(
                    state = prepared.codeBlockState,
                    modifier = Modifier.fillMaxSize()
                )
                CopyButton(
                    textProvider = { prepared.codeBlockState.text },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }
            DebugCodeBenchPanel(title = "CodeBlock", entries = prepared.generation.codeBlockBench)
        } else {
            LoadingPlaceholder(
                message = I18n.get("debug:code.placeholder.codeblock"),
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
        }
    }
}

@Composable
private fun CodeDiffSection(prepared: DebugCodePreparedSample?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
        if (prepared != null) {
            CodeDiff(
                original = prepared.generation.original,
                compiled = prepared.generation.modified,
                status = DiffStatus.UPDATED,
                textStyle = DEBUG_CODE_MONO_STYLE,
                lineSpacing = 5f,
                borderFill = StudioColors.Zinc800,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            DebugCodeBenchPanel(title = "CodeDiff", entries = prepared.generation.codeDiffBench)
        } else {
            LoadingPlaceholder(
                message = I18n.get("debug:code.placeholder.diff"),
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
        }
    }
}

@Composable
private fun CodeEditorSection(prepared: DebugCodePreparedSample?) {
    if (prepared != null) {
        CodeEditor(state = prepared.editorState, modifier = Modifier.fillMaxSize())
    } else {
        LoadingPlaceholder(
            message = I18n.get("debug:code.placeholder.editor"),
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun tabHeader(tab: String): Pair<String, String> = when (tab) {
    TAB_CODEBLOCK -> I18n.get("debug:code.title") to I18n.get("debug:code.description")
    TAB_DIFF -> I18n.get("debug:code.diff.title") to I18n.get("debug:code.diff.description")
    TAB_EDITOR -> I18n.get("debug:code.editor.title") to I18n.get("debug:code.editor.description")
    else -> "" to ""
}

private fun icon(name: String): Identifier =
    Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/$name.svg")

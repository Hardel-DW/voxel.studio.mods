package fr.hardel.asset_editor.client.compose.routes.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.page.debug.DEBUG_CODE_BLOCK_MONO_STYLE
import fr.hardel.asset_editor.client.compose.components.page.debug.DebugCodeBlockBenchPanel
import fr.hardel.asset_editor.client.compose.components.page.debug.DebugCodeBlockPreparedSample
import fr.hardel.asset_editor.client.compose.components.page.debug.prepareDebugCodeBlockSample
import fr.hardel.asset_editor.client.compose.components.ui.Button
import fr.hardel.asset_editor.client.compose.components.ui.ButtonSize
import fr.hardel.asset_editor.client.compose.components.ui.ButtonVariant
import fr.hardel.asset_editor.client.compose.components.ui.CopyButton
import fr.hardel.asset_editor.client.compose.components.ui.InputText
import fr.hardel.asset_editor.client.compose.components.ui.LoadingPlaceholder
import fr.hardel.asset_editor.client.compose.components.ui.Section
import fr.hardel.asset_editor.client.compose.components.ui.codeblock.CodeBlock
import fr.hardel.asset_editor.client.compose.components.ui.codeblock.CodeDiff
import fr.hardel.asset_editor.client.compose.components.ui.codeblock.DiffStatus
import fr.hardel.asset_editor.client.compose.components.ui.editor.CodeEditor
import net.minecraft.client.resources.language.I18n

@Composable
fun DebugCodeBlockPage() {
    var lineCountInput by remember { mutableStateOf("256") }
    var prepared by remember { mutableStateOf<DebugCodeBlockPreparedSample?>(null) }
    var isGenerating by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        prepared = prepareDebugCodeBlockSample(256)
        isGenerating = false
    }

    val generation = prepared?.generation
    val state = prepared?.codeBlockState
    val editorState = prepared?.editorState

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Section(I18n.get("debug:code.title")) {
                Text(
                    text = I18n.get("debug:code.description"),
                    style = StudioTypography.regular(13),
                    color = StudioColors.Zinc400
                )
            }
            CopyButton(textProvider = { state?.text.orEmpty() })
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            InputText(
                value = lineCountInput,
                onValueChange = { lineCountInput = it.filter { c -> c.isDigit() }.take(7) },
                placeholder = I18n.get("debug:code.input.placeholder"),
                showSearchIcon = false,
                modifier = Modifier.widthIn(max = 140.dp)
            )
            Button(
                onClick = {
                    if (isGenerating) return@Button
                    val n = lineCountInput.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    isGenerating = true
                    coroutineScope.launch {
                        prepared = prepareDebugCodeBlockSample(n)
                        isGenerating = false
                    }
                },
                text = I18n.get(
                    if (isGenerating) "debug:code.action.generating" else "debug:code.action.generate"
                ),
                variant = ButtonVariant.GHOST_BORDER,
                size = ButtonSize.SM,
                enabled = !isGenerating
            )
            val statusText = when {
                isGenerating && generation == null -> I18n.get("debug:code.status.preparing")
                generation != null -> {
                    val summary = I18n.get(
                        "debug:code.status.summary",
                        generation.lineCount,
                        generation.charCount,
                        generation.genMillis,
                        generation.diffMillis
                    )
                    if (isGenerating) summary + I18n.get("debug:code.status.regenerating") else summary
                }
                else -> ""
            }
            Text(text = statusText, style = StudioTypography.regular(12), color = StudioColors.Zinc500)
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(520.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state != null && generation != null) {
                    CodeBlock(
                        state = state,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                    DebugCodeBlockBenchPanel(title = "CodeBlock", entries = generation.codeBlockBench)
                } else {
                    LoadingPlaceholder(
                        message = I18n.get("debug:code.placeholder.codeblock"),
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (generation != null) {
                    CodeDiff(
                        original = generation.original,
                        compiled = generation.modified,
                        status = DiffStatus.UPDATED,
                        textStyle = DEBUG_CODE_BLOCK_MONO_STYLE,
                        lineSpacing = 5f,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                    DebugCodeBlockBenchPanel(title = "CodeDiff", entries = generation.codeDiffBench)
                } else {
                    LoadingPlaceholder(
                        message = I18n.get("debug:code.placeholder.diff"),
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                }
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(640.dp)
        ) {
            Text(
                text = I18n.get("debug:code.editor.title"),
                style = StudioTypography.medium(11),
                color = StudioColors.Zinc500
            )
            if (editorState != null) {
                CodeEditor(state = editorState, modifier = Modifier.fillMaxWidth().weight(1f))
            } else {
                LoadingPlaceholder(
                    message = I18n.get("debug:code.placeholder.editor"),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            }
        }
    }
}

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import com.mojang.serialization.codecs.RecordCodecBuilder
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.Button
import fr.hardel.asset_editor.client.compose.components.ui.ButtonSize
import fr.hardel.asset_editor.client.compose.components.ui.ButtonVariant
import fr.hardel.asset_editor.client.compose.components.ui.CopyButton
import fr.hardel.asset_editor.client.compose.components.ui.DataTable
import fr.hardel.asset_editor.client.compose.components.ui.InputText
import fr.hardel.asset_editor.client.compose.components.ui.LoadingPlaceholder
import fr.hardel.asset_editor.client.compose.components.ui.Section
import fr.hardel.asset_editor.client.compose.components.ui.TableColumn
import fr.hardel.asset_editor.client.compose.components.ui.codeblock.CodeBlock
import fr.hardel.asset_editor.client.compose.components.ui.codeblock.CodeBlockState
import fr.hardel.asset_editor.client.compose.components.ui.codeblock.CodeDiff
import fr.hardel.asset_editor.client.compose.components.ui.codeblock.DiffStatus
import fr.hardel.asset_editor.client.compose.components.ui.codeblock.JsonCodeBlockHighlighter
import fr.hardel.asset_editor.client.compose.components.ui.codeblock.prepareCodeBlockAsync
import fr.hardel.asset_editor.client.compose.components.ui.editor.CodeEditor
import fr.hardel.asset_editor.client.compose.components.ui.editor.CodeEditorState
import java.util.concurrent.atomic.AtomicReference
import net.minecraft.client.resources.language.I18n

private val PRETTY_GSON = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
private val MONO_STYLE = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp)

/** Gson's default pretty printer uses two-space indent — match it in the editor. */
private const val GSON_INDENT = "  "

@Composable
fun DebugCodeBlockPage() {
    var lineCountInput by remember { mutableStateOf("256") }
    var prepared by remember { mutableStateOf<PreparedDebugSample?>(null) }
    var isGenerating by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        prepared = prepareDebugSample(256)
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
                        prepareDebugSample(n)
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
            Text(
                text = statusText,
                style = StudioTypography.regular(12),
                color = StudioColors.Zinc500
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(VIEWER_ROW_HEIGHT)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state != null && generation != null) {
                    CodeBlock(
                        state = state,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                    BenchPanel(
                        title = "CodeBlock",
                        entries = generation.codeBlockBench
                    )
                } else {
                    LoadingPlaceholder(
                        message = I18n.get("debug:code.placeholder.codeblock"),
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (generation != null) {
                    CodeDiff(
                        original = generation.original,
                        compiled = generation.modified,
                        status = DiffStatus.UPDATED,
                        textStyle = MONO_STYLE,
                        lineSpacing = 5f,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                    BenchPanel(
                        title = "CodeDiff",
                        entries = generation.codeDiffBench
                    )
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
                .height(EDITOR_ROW_HEIGHT)
        ) {
            Text(
                text = I18n.get("debug:code.editor.title"),
                style = StudioTypography.medium(11),
                color = StudioColors.Zinc500
            )
            if (editorState != null) {
                CodeEditor(
                    state = editorState,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            } else {
                LoadingPlaceholder(
                    message = I18n.get("debug:code.placeholder.editor"),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            }
        }
    }
}

private val VIEWER_ROW_HEIGHT = 520.dp
private val EDITOR_ROW_HEIGHT = 640.dp

@Composable
private fun BenchPanel(title: String, entries: List<BenchEntry>) {
    val rows = remember(entries) { entries }
    DataTable(
        items = rows,
        columns = listOf(
            TableColumn(header = I18n.get("debug:code.bench.column.stage", title), weight = 2f) { entry ->
                Text(
                    text = entry.stage,
                    style = StudioTypography.regular(11).copy(fontFamily = FontFamily.Monospace),
                    color = StudioColors.Zinc400
                )
            },
            TableColumn(header = I18n.get("debug:code.bench.column.ms"), weight = 1f) { entry ->
                Text(
                    text = I18n.get("debug:code.bench.value", entry.millis),
                    style = StudioTypography.regular(11).copy(fontFamily = FontFamily.Monospace),
                    color = if (entry.millis > 50) StudioColors.Zinc200 else StudioColors.Zinc500
                )
            }
        ),
        modifier = Modifier.fillMaxWidth().height(140.dp)
    )
}

private fun configureDebugCodeBlock(state: CodeBlockState) {
    JsonCodeBlockHighlighter.installDefaultPalette(state.palette)
    state.highlighter = JsonCodeBlockHighlighter()
    state.textFill = StudioColors.Zinc300
    state.backgroundFill = StudioColors.Zinc950
    state.borderFill = StudioColors.Zinc800
    state.textStyle = MONO_STYLE
    state.lineSpacing = 5.sp
    state.wrapText = false
    state.minHeight = 360.dp
    state.showLineNumbers = true
}

private data class PreparedDebugSample(
    val generation: SampleGeneration,
    val codeBlockState: CodeBlockState,
    val editorState: CodeEditorState
)

/**
 * Builds the entire debug sample — JSON encode, histogram pre-warm,
 * [CodeBlockState] tokenization + global `AnnotatedString`, [CodeEditorState]
 * with a pre-warmed line cache — on a background dispatcher so the main
 * thread only sees a single state swap when it's done.
 */
private suspend fun prepareDebugSample(targetLines: Int): PreparedDebugSample =
    withContext(Dispatchers.Default) {
        val gen = generateSample(targetLines)
        val codeBlockState = prepareCodeBlockAsync(gen.original) {
            configureDebugCodeBlock(this)
        }
        val editorState = CodeEditorState(gen.original).apply {
            minHeight = 360.dp
            indentUnit = GSON_INDENT
            prewarmCache(maxLines = 256)
        }
        PreparedDebugSample(gen, codeBlockState, editorState)
    }

private data class SampleGeneration(
    val original: String,
    val modified: String,
    val lineCount: Int,
    val charCount: Int,
    val genMillis: Long,
    val diffMillis: Long,
    val codeBlockBench: List<BenchEntry>,
    val codeDiffBench: List<BenchEntry>
)

/**
 * Builds an `original` / `modified` JSON pair of approximately [targetLines] lines
 * each and pre-warms a histogram diff so the header can surface the raw cost.
 * The actual diff used by the visible [CodeDiff] still runs through `remember`.
 */
private fun generateSample(targetLines: Int): SampleGeneration {
    val gen = BenchTimings()
    val original = gen.measure("encode original") { encodeSampleJson(targetLines, modified = false) }
    val modified = gen.measure("encode modified") { encodeSampleJson(targetLines, modified = true) }
    val lineCount = original.count { it == '\n' } + 1

    val diffBench = BenchTimings()
    diffBench.measure("histogram diff") {
        fr.hardel.asset_editor.client.compose.lib.utils.DiffComputer
            .computeUnifiedDiff(original, modified)
    }

    val genStages = gen.snapshot()
    val diffStages = diffBench.snapshot()

    return SampleGeneration(
        original = original,
        modified = modified,
        lineCount = lineCount,
        charCount = original.length,
        genMillis = genStages.sumOf { it.millis },
        diffMillis = diffStages.sumOf { it.millis },
        codeBlockBench = genStages,
        codeDiffBench = diffStages
    )
}

private fun encodeSampleJson(targetLines: Int, modified: Boolean): String {
    val payload = buildSamplePayload(targetLines, modified)
    val encoded = AtomicReference("{}")
    SamplePayload.CODEC.encodeStart(JsonOps.INSTANCE, payload).ifSuccess { json ->
        encoded.set(prettyPrint(json))
    }
    return encoded.get()
}

private fun prettyPrint(json: JsonElement): String = PRETTY_GSON.toJson(json)

/**
 * A pretty-printed [SamplePayload] with N projects is roughly `(N * 4) + 15` lines,
 * so we invert that to hit the requested line count.
 */
private fun buildSamplePayload(targetLines: Int, modified: Boolean): SamplePayload {
    val projectCount = ((targetLines - 15).coerceAtLeast(0) / 4).coerceAtLeast(1)
    val projects = (1..projectCount).map { idx ->
        val name = if (modified && idx % 7 == 0) "Project $idx (renamed)" else "Project $idx"
        Project(idx, name)
    }
    val baseRoles = listOf("admin", "user")
    val roles = if (modified) baseRoles + "moderator" else baseRoles
    return SamplePayload(
        name = "John Doe",
        email = if (modified) "john.doe@company.com" else "john@example.com",
        age = if (modified) 31 else 30,
        active = true,
        roles = roles,
        address = Address(
            street = if (modified) "456 Oak Ave" else "123 Main St",
            city = if (modified) "San Francisco" else "New York",
            country = "USA"
        ),
        projects = projects
    )
}

private data class Address(val street: String, val city: String, val country: String) {
    companion object {
        val CODEC: Codec<Address> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("street").forGetter(Address::street),
                Codec.STRING.fieldOf("city").forGetter(Address::city),
                Codec.STRING.fieldOf("country").forGetter(Address::country)
            ).apply(instance, ::Address)
        }
    }
}

private data class Project(val id: Int, val name: String) {
    companion object {
        val CODEC: Codec<Project> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.INT.fieldOf("id").forGetter(Project::id),
                Codec.STRING.fieldOf("name").forGetter(Project::name)
            ).apply(instance, ::Project)
        }
    }
}

private data class SamplePayload(
    val name: String,
    val email: String,
    val age: Int,
    val active: Boolean,
    val roles: List<String>,
    val address: Address,
    val projects: List<Project>
) {
    companion object {
        val CODEC: Codec<SamplePayload> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("name").forGetter(SamplePayload::name),
                Codec.STRING.fieldOf("email").forGetter(SamplePayload::email),
                Codec.INT.fieldOf("age").forGetter(SamplePayload::age),
                Codec.BOOL.fieldOf("active").forGetter(SamplePayload::active),
                Codec.STRING.listOf().fieldOf("roles").forGetter(SamplePayload::roles),
                Address.CODEC.fieldOf("address").forGetter(SamplePayload::address),
                Project.CODEC.listOf().fieldOf("projects").forGetter(SamplePayload::projects)
            ).apply(instance, ::SamplePayload)
        }
    }
}

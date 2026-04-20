package fr.hardel.asset_editor.client.compose.components.page.debug.code

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import com.mojang.serialization.codecs.RecordCodecBuilder
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.components.ui.codeblock.CodeBlockState
import fr.hardel.asset_editor.client.compose.components.ui.codeblock.JsonCodeBlockHighlighter
import fr.hardel.asset_editor.client.compose.components.ui.codeblock.prepareCodeBlockAsync
import fr.hardel.asset_editor.client.compose.components.ui.editor.CodeEditorState
import fr.hardel.asset_editor.client.compose.lib.utils.DiffComputer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference

val DEBUG_CODE_MONO_STYLE = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp)
private val DEBUG_CODE_GSON = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
private const val DEBUG_CODE_GSON_INDENT = "  "

data class DebugCodeBenchEntry(val stage: String, val millis: Long)

data class DebugCodeSampleGeneration(
    val original: String,
    val modified: String,
    val lineCount: Int,
    val charCount: Int,
    val genMillis: Long,
    val diffMillis: Long,
    val codeBlockBench: List<DebugCodeBenchEntry>,
    val codeDiffBench: List<DebugCodeBenchEntry>
)

data class DebugCodePreparedSample(
    val generation: DebugCodeSampleGeneration,
    val codeBlockState: CodeBlockState,
    val editorState: CodeEditorState
)

suspend fun prepareDebugCodeSample(targetLines: Int): DebugCodePreparedSample =
    withContext(Dispatchers.Default) {
        val gen = generateSample(targetLines)
        val codeBlockState = prepareCodeBlockAsync(gen.original) { configureCodeBlock(this) }
        val editorState = CodeEditorState(gen.original).apply {
            minHeight = 360.dp
            borderFill = StudioColors.Zinc800
            indentUnit = DEBUG_CODE_GSON_INDENT
            prewarmCache(maxLines = 256)
        }
        DebugCodePreparedSample(gen, codeBlockState, editorState)
    }

private fun configureCodeBlock(state: CodeBlockState) {
    JsonCodeBlockHighlighter.installDefaultPalette(state.palette)
    state.highlighter = JsonCodeBlockHighlighter()
    state.textFill = StudioColors.Zinc300
    state.backgroundFill = StudioColors.Zinc950
    state.borderFill = StudioColors.Zinc800
    state.textStyle = DEBUG_CODE_MONO_STYLE
    state.lineSpacing = 5.sp
    state.wrapText = false
    state.minHeight = 360.dp
    state.showLineNumbers = true
}

private fun generateSample(targetLines: Int): DebugCodeSampleGeneration {
    val gen = BenchTimings()
    val original = gen.measure("encode original") { encodeSampleJson(targetLines, modified = false) }
    val modified = gen.measure("encode modified") { encodeSampleJson(targetLines, modified = true) }
    val lineCount = original.count { it == '\n' } + 1

    val diffBench = BenchTimings()
    diffBench.measure("histogram diff") { DiffComputer.computeUnifiedDiff(original, modified) }

    val genStages = gen.snapshot()
    val diffStages = diffBench.snapshot()

    return DebugCodeSampleGeneration(
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
    DebugSamplePayload.CODEC.encodeStart(JsonOps.INSTANCE, payload).ifSuccess { json ->
        encoded.set(prettyPrint(json))
    }
    return encoded.get()
}

private fun prettyPrint(json: JsonElement): String = DEBUG_CODE_GSON.toJson(json)

private fun buildSamplePayload(targetLines: Int, modified: Boolean): DebugSamplePayload {
    val projectCount = ((targetLines - 15).coerceAtLeast(0) / 4).coerceAtLeast(1)
    val projects = (1..projectCount).map { idx ->
        val name = if (modified && idx % 7 == 0) "Project $idx (renamed)" else "Project $idx"
        DebugSampleProject(idx, name)
    }
    val baseRoles = listOf("admin", "user")
    val roles = if (modified) baseRoles + "moderator" else baseRoles
    return DebugSamplePayload(
        name = "John Doe",
        email = if (modified) "john.doe@company.com" else "john@example.com",
        age = if (modified) 31 else 30,
        active = true,
        roles = roles,
        address = DebugSampleAddress(
            street = if (modified) "456 Oak Ave" else "123 Main St",
            city = if (modified) "San Francisco" else "New York",
            country = "USA"
        ),
        projects = projects
    )
}

private class BenchTimings {
    private val entries = LinkedHashMap<String, Long>()

    fun <T> measure(stage: String, block: () -> T): T {
        val start = System.nanoTime()
        val result = block()
        entries[stage] = (System.nanoTime() - start) / 1_000_000
        return result
    }

    fun snapshot(): List<DebugCodeBenchEntry> = entries.map { (stage, ms) -> DebugCodeBenchEntry(stage, ms) }
}

private data class DebugSampleAddress(val street: String, val city: String, val country: String) {
    companion object {
        val CODEC: Codec<DebugSampleAddress> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("street").forGetter(DebugSampleAddress::street),
                Codec.STRING.fieldOf("city").forGetter(DebugSampleAddress::city),
                Codec.STRING.fieldOf("country").forGetter(DebugSampleAddress::country)
            ).apply(instance, ::DebugSampleAddress)
        }
    }
}

private data class DebugSampleProject(val id: Int, val name: String) {
    companion object {
        val CODEC: Codec<DebugSampleProject> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.INT.fieldOf("id").forGetter(DebugSampleProject::id),
                Codec.STRING.fieldOf("name").forGetter(DebugSampleProject::name)
            ).apply(instance, ::DebugSampleProject)
        }
    }
}

private data class DebugSamplePayload(
    val name: String,
    val email: String,
    val age: Int,
    val active: Boolean,
    val roles: List<String>,
    val address: DebugSampleAddress,
    val projects: List<DebugSampleProject>
) {
    companion object {
        val CODEC: Codec<DebugSamplePayload> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("name").forGetter(DebugSamplePayload::name),
                Codec.STRING.fieldOf("email").forGetter(DebugSamplePayload::email),
                Codec.INT.fieldOf("age").forGetter(DebugSamplePayload::age),
                Codec.BOOL.fieldOf("active").forGetter(DebugSamplePayload::active),
                Codec.STRING.listOf().fieldOf("roles").forGetter(DebugSamplePayload::roles),
                DebugSampleAddress.CODEC.fieldOf("address").forGetter(DebugSamplePayload::address),
                DebugSampleProject.CODEC.listOf().fieldOf("projects").forGetter(DebugSamplePayload::projects)
            ).apply(instance, ::DebugSamplePayload)
        }
    }
}

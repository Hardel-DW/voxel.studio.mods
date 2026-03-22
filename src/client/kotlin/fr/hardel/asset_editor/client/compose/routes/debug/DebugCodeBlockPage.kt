package fr.hardel.asset_editor.client.compose.routes.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import com.mojang.serialization.codecs.RecordCodecBuilder
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import fr.hardel.asset_editor.client.compose.components.ui.CopyButton
import fr.hardel.asset_editor.client.compose.components.ui.Section
import fr.hardel.asset_editor.client.compose.components.ui.codeblock.CodeBlock
import fr.hardel.asset_editor.client.compose.components.ui.codeblock.JsonCodeBlockHighlighter
import fr.hardel.asset_editor.client.compose.components.ui.codeblock.rememberCodeBlockState
import java.util.concurrent.atomic.AtomicReference
import net.minecraft.client.resources.language.I18n

@Composable
fun DebugCodeBlockPage() {
    val state = rememberCodeBlockState()

    LaunchedEffect(Unit) {
        JsonCodeBlockHighlighter.installDefaultPalette(state.palette)
        state.highlighter = JsonCodeBlockHighlighter()
        state.textFill = VoxelColors.Zinc300
        state.backgroundFill = VoxelColors.Zinc960
        state.borderFill = VoxelColors.Zinc800
        state.textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp)
        state.contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp)
        state.lineSpacing = 5.sp
        state.wrapText = false
        state.minHeight = 360.dp
        state.text = encodeSampleJson()
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp)
    ) {
        Section(I18n.get("debug:code.title")) {
            androidx.compose.material.Text(
                text = I18n.get("debug:code.description"),
                style = VoxelTypography.regular(13),
                color = VoxelColors.Zinc400
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.weight(1f))
                CopyButton(textProvider = { state.text })
            }

            CodeBlock(
                state = state,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun encodeSampleJson(): String {
    val encoded = AtomicReference("{}")
    SamplePayload.CODEC.encodeStart(JsonOps.INSTANCE, SAMPLE_PAYLOAD).ifSuccess { json ->
        encoded.set(prettyPrint(json))
    }
    return encoded.get()
}

private fun prettyPrint(json: JsonElement): String =
    GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(json)

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

private val SAMPLE_PAYLOAD = SamplePayload(
    name = "John Doe",
    email = "john@example.com",
    age = 30,
    active = true,
    roles = listOf("admin", "user"),
    address = Address("123 Main St", "New York", "USA"),
    projects = listOf(Project(1, "Website Redesign"), Project(2, "Mobile App"))
)

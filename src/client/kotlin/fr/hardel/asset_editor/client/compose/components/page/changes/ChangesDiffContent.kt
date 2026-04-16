package fr.hardel.asset_editor.client.compose.components.page.changes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.Badge
import fr.hardel.asset_editor.client.compose.components.ui.FloatingBanner
import fr.hardel.asset_editor.client.compose.components.ui.codeblock.CodeDiff
import fr.hardel.asset_editor.client.compose.components.ui.codeblock.DiffStatus
import fr.hardel.asset_editor.client.compose.lib.git.GitDiffPayload
import fr.hardel.asset_editor.client.compose.lib.git.GitFileStatus
import fr.hardel.asset_editor.client.compose.lib.git.GitState
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val BANNER_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/pencil.svg")
private val BANNER_ACCENT = Color(0xFF38BDF8)

@Composable
fun ChangesDiffContent(
    state: GitState,
    selectedFile: String?,
    modifier: Modifier = Modifier
) {
    val status = state.snapshot.status[selectedFile]
    if (selectedFile.isNullOrBlank() || !isPreviewable(selectedFile) || status == null) {
        DiffEmptyState(selectedFile = selectedFile, modifier = modifier)
        return
    }

    var payload by remember(selectedFile) { mutableStateOf<GitDiffPayload?>(null) }
    LaunchedEffect(selectedFile, state.snapshot.root) {
        payload = state.readDiff(selectedFile)
    }

    val name = selectedFile.substringAfterLast('/')
    Column(modifier = modifier.fillMaxSize()) {
        DiffHeader(name = name, file = selectedFile)
        Box(modifier = Modifier.fillMaxSize()) {
            val diff = payload
            if (diff == null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = I18n.get("changes:diff.loading"),
                        style = StudioTypography.regular(12),
                        color = StudioColors.Zinc500
                    )
                }
            } else {
                val formattedOriginal = formatIfJson(selectedFile, diff.original)
                val formattedWorking = formatIfJson(selectedFile, diff.working)
                DiffBody(
                    status = status,
                    original = formattedOriginal,
                    working = formattedWorking,
                    modifier = Modifier.fillMaxSize().padding(bottom = 24.dp)
                )
            }
            FloatingBanner(
                icon = BANNER_ICON,
                accent = BANNER_ACCENT,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    Badge(text = I18n.get("changes:banner.diff.badge"), accent = BANNER_ACCENT)
                    Text(
                        text = I18n.get("changes:banner.diff.title"),
                        style = StudioTypography.bold(13),
                        color = Color(0xFFF4F4F5)
                    )
                }
                Text(
                    text = I18n.get("changes:banner.diff.description"),
                    style = StudioTypography.regular(12),
                    color = StudioColors.Zinc400
                )
            }
        }
    }
}

@Composable
private fun DiffBody(
    status: GitFileStatus,
    original: String,
    working: String,
    modifier: Modifier = Modifier
) {
    when (status) {
        GitFileStatus.ADDED, GitFileStatus.UNTRACKED -> CodeDiff(
            original = "",
            compiled = working,
            status = DiffStatus.ADDED,
            modifier = modifier
        )
        GitFileStatus.DELETED -> CodeDiff(
            original = original,
            compiled = "",
            status = DiffStatus.DELETED,
            modifier = modifier
        )
        GitFileStatus.MODIFIED, GitFileStatus.RENAMED -> CodeDiff(
            original = original,
            compiled = working,
            status = DiffStatus.UPDATED,
            modifier = modifier
        )
    }
}

private fun isPreviewable(path: String): Boolean =
    path.endsWith(".json") || path.endsWith(".mcfunction") || path.endsWith(".mcmeta")

private fun formatIfJson(path: String, content: String): String {
    if (!path.endsWith(".json") && !path.endsWith(".mcmeta")) return content
    if (content.isBlank()) return content
    return runCatching {
        val element = com.google.gson.JsonParser.parseString(content)
        com.google.gson.GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(element)
    }.getOrElse { content }
}

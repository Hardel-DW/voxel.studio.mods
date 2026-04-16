package fr.hardel.asset_editor.client.compose.routes.changes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.page.changes.ChangesDiffBody
import fr.hardel.asset_editor.client.compose.components.page.changes.DiffEmptyState
import fr.hardel.asset_editor.client.compose.components.page.changes.DiffHeader
import fr.hardel.asset_editor.client.compose.lib.git.GitDiffPayload
import fr.hardel.asset_editor.client.compose.lib.git.GitState
import net.minecraft.client.resources.language.I18n

@Composable
fun ChangesPage(gitState: GitState, selectedFile: String?) {
    val status = gitState.snapshot.status[selectedFile]
    val previewable = selectedFile != null &&
        (selectedFile.endsWith(".json") || selectedFile.endsWith(".mcfunction") || selectedFile.endsWith(".mcmeta"))
    if (selectedFile.isNullOrBlank() || !previewable || status == null) {
        DiffEmptyState(selectedFile = selectedFile, modifier = Modifier.fillMaxSize())
        return
    }

    var payload by remember(selectedFile) { mutableStateOf<GitDiffPayload?>(null) }
    LaunchedEffect(selectedFile, gitState.snapshot.root) {
        payload = gitState.readDiff(selectedFile)
    }

    val name = selectedFile.substringAfterLast('/')

    Column(modifier = Modifier.fillMaxSize()) {
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
                ChangesDiffBody(
                    status = status,
                    original = formatDiffContentIfJson(selectedFile, diff.original),
                    working = formatDiffContentIfJson(selectedFile, diff.working),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

private fun formatDiffContentIfJson(path: String, content: String): String {
    if (!path.endsWith(".json") && !path.endsWith(".mcmeta")) return content
    if (content.isBlank()) return content
    return runCatching {
        val element = JsonParser.parseString(content)
        GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(element)
    }.getOrElse { content }
}

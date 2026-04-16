package fr.hardel.asset_editor.client.compose.routes.changes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import fr.hardel.asset_editor.client.compose.components.page.changes.ChangesDiffBody
import fr.hardel.asset_editor.client.compose.components.page.changes.DiffEmptyState
import fr.hardel.asset_editor.client.compose.components.page.changes.DiffHeader
import fr.hardel.asset_editor.client.compose.components.page.changes.formatDiffContentIfJson
import fr.hardel.asset_editor.client.compose.components.page.changes.isPreviewableDiffPath
import fr.hardel.asset_editor.client.compose.components.ui.Badge
import fr.hardel.asset_editor.client.compose.components.ui.FloatingBanner
import fr.hardel.asset_editor.client.compose.lib.git.GitDiffPayload
import fr.hardel.asset_editor.client.compose.lib.git.GitState
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val BANNER_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/pencil.svg")
private val BANNER_ACCENT = Color(0xFF38BDF8)

@Composable
fun ChangesPage(gitState: GitState, selectedFile: String?) {
    val status = gitState.snapshot.status[selectedFile]
    if (selectedFile.isNullOrBlank() || !isPreviewableDiffPath(selectedFile) || status == null) {
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
                    modifier = Modifier.fillMaxSize().padding(bottom = 24.dp)
                )
            }

            FloatingBanner(
                icon = BANNER_ICON,
                accent = BANNER_ACCENT,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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

package fr.hardel.asset_editor.client.compose.routes.changes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.google.gson.JsonParser
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.Button
import fr.hardel.asset_editor.client.compose.components.ui.ButtonSize
import fr.hardel.asset_editor.client.compose.components.ui.ButtonVariant
import fr.hardel.asset_editor.client.compose.components.ui.codeblock.CodeBlock
import fr.hardel.asset_editor.client.compose.components.ui.codeblock.CodeBlockState
import fr.hardel.asset_editor.client.compose.components.ui.codeblock.JsonCodeBlockHighlighter
import fr.hardel.asset_editor.client.compose.components.ui.codeblock.prepareCodeBlockAsync
import fr.hardel.asset_editor.client.compose.lib.git.GitState
import fr.hardel.asset_editor.client.compose.lib.git.OperationInProgress
import net.minecraft.client.resources.language.I18n

private const val STAGE_OURS = 2
private const val STAGE_THEIRS = 3

@Composable
fun ConflictResolutionPage(
    gitState: GitState,
    path: String,
    onAcceptOurs: () -> Unit,
    onAcceptTheirs: () -> Unit
) {
    val snapshot = gitState.snapshot
    val current = snapshot.currentBranch
    val incoming = snapshot.incomingBranch
    val operation = snapshot.operationInProgress
    val isJson = path.endsWith(".json") || path.endsWith(".mcmeta")

    var oursState by remember(path) { mutableStateOf<CodeBlockState?>(null) }
    var theirsState by remember(path) { mutableStateOf<CodeBlockState?>(null) }
    var oursMissing by remember(path) { mutableStateOf(false) }
    var theirsMissing by remember(path) { mutableStateOf(false) }

    LaunchedEffect(path, snapshot.status[path]) {
        val ours = gitState.readConflictStage(path, STAGE_OURS)
        val theirs = gitState.readConflictStage(path, STAGE_THEIRS)
        oursMissing = ours == null
        theirsMissing = theirs == null
        oursState = ours?.let { preview -> buildCodeBlock(preview, isJson) }
        theirsState = theirs?.let { preview -> buildCodeBlock(preview, isJson) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ConflictHeader(path = path, operation = operation)

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            ConflictColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                branch = current ?: "HEAD",
                role = I18n.get("changes:conflict.role.current"),
                state = oursState,
                missing = oursMissing,
                acceptLabel = I18n.get("changes:conflict.accept.current"),
                onAccept = onAcceptOurs
            )
            ConflictColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                branch = incoming ?: I18n.get("changes:conflict.fallback.incoming"),
                role = I18n.get("changes:conflict.role.incoming"),
                state = theirsState,
                missing = theirsMissing,
                acceptLabel = I18n.get("changes:conflict.accept.incoming"),
                onAccept = onAcceptTheirs
            )
        }
    }
}

@Composable
private fun ConflictHeader(path: String, operation: OperationInProgress?) {
    val name = path.substringAfterLast('/')
    val subtitle = when (operation) {
        OperationInProgress.MERGE -> I18n.get("changes:conflict.header.merge")
        OperationInProgress.REBASE -> I18n.get("changes:conflict.header.rebase")
        OperationInProgress.CHERRY_PICK -> I18n.get("changes:conflict.header.cherry_pick")
        null -> ""
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = name,
                style = StudioTypography.semiBold(14),
                color = StudioColors.Zinc100
            )
            Text(
                text = path,
                style = StudioTypography.regular(11),
                color = StudioColors.Zinc500
            )
        }
        ConflictBadge(label = subtitle)
    }
}

@Composable
private fun ConflictBadge(label: String) {
    val shape = RoundedCornerShape(6.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .background(StudioColors.Red400.copy(alpha = 0.1f), shape)
            .border(1.dp, StudioColors.Red400.copy(alpha = 0.3f), shape)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = I18n.get("changes:conflict.badge"),
            style = StudioTypography.medium(10),
            color = StudioColors.Red300
        )
        if (label.isNotBlank()) {
            Text(
                text = label,
                style = StudioTypography.regular(10),
                color = StudioColors.Zinc500
            )
        }
    }
}

@Composable
private fun ConflictColumn(
    modifier: Modifier,
    branch: String,
    role: String,
    state: CodeBlockState?,
    missing: Boolean,
    acceptLabel: String,
    onAccept: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .background(StudioColors.Zinc950.copy(alpha = 0.5f), shape)
            .border(1.dp, StudioColors.Zinc800.copy(alpha = 0.6f), shape)
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = branch,
                style = StudioTypography.semiBold(13),
                color = StudioColors.Zinc100
            )
            Text(
                text = role.uppercase(),
                style = StudioTypography.medium(9),
                color = StudioColors.Zinc500
            )
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when {
                missing -> MissingState()
                state != null -> CodeBlock(state = state, modifier = Modifier.fillMaxSize())
                else -> LoadingState()
            }
        }

        Button(
            onClick = onAccept,
            variant = ButtonVariant.DEFAULT,
            size = ButtonSize.SM,
            enabled = !missing,
            text = acceptLabel,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun LoadingState() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Text(
            text = I18n.get("changes:conflict.loading"),
            style = StudioTypography.regular(11),
            color = StudioColors.Zinc500
        )
    }
}

@Composable
private fun MissingState() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(
            text = I18n.get("changes:conflict.missing"),
            style = StudioTypography.regular(11),
            color = StudioColors.Zinc500
        )
    }
}

private suspend fun buildCodeBlock(raw: String, isJson: Boolean): CodeBlockState {
    val formatted = if (isJson) formatJsonSafe(raw) else raw
    return prepareCodeBlockAsync(formatted) {
        borderFill = StudioColors.Zinc800.copy(alpha = 0.4f)
        showLineNumbers = true
        if (isJson) highlighter = JsonCodeBlockHighlighter()
    }
}

private fun formatJsonSafe(content: String): String {
    if (content.isBlank()) return content
    return runCatching {
        val element = JsonParser.parseString(content)
        GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(element)
    }.getOrElse { content }
}


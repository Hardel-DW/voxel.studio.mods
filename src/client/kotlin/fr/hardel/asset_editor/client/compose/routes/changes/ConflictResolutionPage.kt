package fr.hardel.asset_editor.client.compose.routes.changes

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.ToggleGroup
import fr.hardel.asset_editor.client.compose.components.ui.ToggleOption
import fr.hardel.asset_editor.client.compose.components.ui.codeblock.CodeBlock
import fr.hardel.asset_editor.client.compose.components.ui.codeblock.CodeBlockState
import fr.hardel.asset_editor.client.compose.components.ui.codeblock.CodeDiff
import fr.hardel.asset_editor.client.compose.components.ui.codeblock.DiffStatus
import fr.hardel.asset_editor.client.compose.components.ui.codeblock.JsonCodeBlockHighlighter
import fr.hardel.asset_editor.client.compose.components.ui.codeblock.prepareCodeBlockAsync
import fr.hardel.asset_editor.client.compose.lib.git.GitState
import fr.hardel.asset_editor.client.compose.lib.git.OperationInProgress
import net.minecraft.client.resources.language.I18n

private const val VIEW_SPLIT = "split"
private const val VIEW_DIFF = "diff"

@Composable
fun ConflictResolutionPage(
    gitState: GitState,
    path: String,
    onAcceptOurs: () -> Unit,
    onAcceptTheirs: () -> Unit
) {
    val snapshot = gitState.snapshot
    val operation = snapshot.operationInProgress
    val isRebase = operation == OperationInProgress.REBASE
    val currentLabel = if (isRebase) snapshot.incomingBranch else snapshot.currentBranch
    val incomingLabel = if (isRebase) snapshot.currentBranch else snapshot.incomingBranch
    val isJson = path.endsWith(".json") || path.endsWith(".mcmeta")

    var viewMode by remember { mutableStateOf(VIEW_SPLIT) }
    var currentText by remember(path) { mutableStateOf<String?>(null) }
    var incomingText by remember(path) { mutableStateOf<String?>(null) }
    var currentState by remember(path) { mutableStateOf<CodeBlockState?>(null) }
    var incomingState by remember(path) { mutableStateOf<CodeBlockState?>(null) }
    var currentMissing by remember(path) { mutableStateOf(false) }
    var incomingMissing by remember(path) { mutableStateOf(false) }

    LaunchedEffect(path, snapshot.status[path]) {
        val current = gitState.readCurrentSide(path)
        val incoming = gitState.readIncomingSide(path)
        currentMissing = current == null
        incomingMissing = incoming == null
        currentText = current?.let { if (isJson) formatJsonSafe(it) else it }
        incomingText = incoming?.let { if (isJson) formatJsonSafe(it) else it }
        currentState = currentText?.let { buildCodeBlock(it, isJson) }
        incomingState = incomingText?.let { buildCodeBlock(it, isJson) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ConflictHeader(
            path = path,
            viewMode = viewMode,
            onViewModeChange = { viewMode = it }
        )

        Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp)) {
            when (viewMode) {
                VIEW_DIFF -> DiffView(
                    current = currentText,
                    incoming = incomingText,
                    currentMissing = currentMissing,
                    incomingMissing = incomingMissing
                )

                else -> SplitView(
                    currentBranch = currentLabel,
                    incomingBranch = incomingLabel,
                    currentState = currentState,
                    incomingState = incomingState,
                    currentMissing = currentMissing,
                    incomingMissing = incomingMissing
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            ColoredAcceptButton(
                label = I18n.get("changes:conflict.accept.current"),
                tint = StudioColors.Red500,
                enabled = !currentMissing,
                onClick = onAcceptOurs,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            ColoredAcceptButton(
                label = I18n.get("changes:conflict.accept.incoming"),
                tint = StudioColors.Green500,
                enabled = !incomingMissing,
                onClick = onAcceptTheirs,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
        }
    }
}

@Composable
private fun ConflictHeader(
    path: String,
    viewMode: String,
    onViewModeChange: (String) -> Unit
) {
    val name = path.substringAfterLast('/')
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
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
        ToggleGroup(
            options = listOf(
                ToggleOption.TextOption(VIEW_SPLIT, I18n.get("changes:conflict.view.split")),
                ToggleOption.TextOption(VIEW_DIFF, I18n.get("changes:conflict.view.diff"))
            ),
            selectedValue = viewMode,
            onValueChange = onViewModeChange,
            modifier = Modifier.width(180.dp)
        )
    }
}

@Composable
private fun SplitView(
    currentBranch: String?,
    incomingBranch: String?,
    currentState: CodeBlockState?,
    incomingState: CodeBlockState?,
    currentMissing: Boolean,
    incomingMissing: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        ConflictColumn(
            modifier = Modifier.fillMaxSize().weight(1f),
            branch = currentBranch ?: "HEAD",
            role = I18n.get("changes:conflict.role.current"),
            state = currentState,
            missing = currentMissing
        )
        ConflictColumn(
            modifier = Modifier.fillMaxSize().weight(1f),
            branch = incomingBranch ?: I18n.get("changes:conflict.fallback.incoming"),
            role = I18n.get("changes:conflict.role.incoming"),
            state = incomingState,
            missing = incomingMissing
        )
    }
}

@Composable
private fun DiffView(
    current: String?,
    incoming: String?,
    currentMissing: Boolean,
    incomingMissing: Boolean
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioColors.Zinc950.copy(alpha = 0.5f), shape)
            .border(1.dp, StudioColors.Zinc800.copy(alpha = 0.6f), shape)
            .padding(1.dp)
    ) {
        when {
            currentMissing && incomingMissing -> CenterText(I18n.get("changes:conflict.missing"))
            current == null || incoming == null -> CenterText(I18n.get("changes:conflict.loading"))
            else -> CodeDiff(
                original = current,
                compiled = incoming,
                status = DiffStatus.UPDATED,
                borderFill = StudioColors.Zinc800.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxSize()
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
    missing: Boolean
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
                missing -> CenterText(I18n.get("changes:conflict.missing"))
                state != null -> CodeBlock(state = state, modifier = Modifier.fillMaxSize())
                else -> CenterText(I18n.get("changes:conflict.loading"))
            }
        }
    }
}

@Composable
private fun CenterText(text: String) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(
            text = text,
            style = StudioTypography.regular(11),
            color = StudioColors.Zinc500
        )
    }
}

private suspend fun buildCodeBlock(raw: String, isJson: Boolean): CodeBlockState {
    return prepareCodeBlockAsync(raw) {
        borderFill = StudioColors.Zinc800.copy(alpha = 0.4f)
        showLineNumbers = true
        if (isJson) {
            JsonCodeBlockHighlighter.installDefaultPalette(palette)
            highlighter = JsonCodeBlockHighlighter()
        }
    }
}

private fun formatJsonSafe(content: String): String {
    if (content.isBlank()) return content
    return runCatching {
        val element = JsonParser.parseString(content)
        GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(element)
    }.getOrElse { content }
}

@Composable
private fun ColoredAcceptButton(
    label: String,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val isHovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(8.dp)

    val bg by animateColorAsState(
        targetValue = when {
            !enabled -> Color.Transparent
            isHovered -> tint.copy(alpha = 0.22f)
            else -> tint.copy(alpha = 0.1f)
        },
        animationSpec = StudioMotion.hoverSpec(),
        label = "accept-bg"
    )
    val border by animateColorAsState(
        targetValue = tint.copy(alpha = if (isHovered && enabled) 0.55f else 0.3f),
        animationSpec = StudioMotion.hoverSpec(),
        label = "accept-border"
    )
    val textColor by animateColorAsState(
        targetValue = when {
            !enabled -> StudioColors.Zinc600
            isHovered -> StudioColors.Zinc50
            else -> tint
        },
        animationSpec = StudioMotion.hoverSpec(),
        label = "accept-text"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(36.dp)
            .clip(shape)
            .background(bg, shape)
            .border(1.dp, border, shape)
            .hoverable(interaction, enabled)
            .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .alpha(if (enabled) 1f else 0.5f)
            .padding(horizontal = 14.dp)
    ) {
        Text(
            text = label,
            style = StudioTypography.medium(13),
            color = textColor
        )
    }
}

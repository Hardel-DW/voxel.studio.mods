package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTargetModifierNode
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import fr.hardel.asset_editor.client.compose.pressScale
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTypography
import java.io.File
import java.net.URI
import javax.imageio.ImageIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraft.resources.Identifier
import org.lwjgl.system.MemoryStack
import org.lwjgl.util.tinyfd.TinyFileDialogs

private val UPLOAD_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/plus.svg")
private val FILE_FALLBACK_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/folder.svg")
private val CLOSE_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/close.svg")

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FileInput(
    promptText: String,
    onFileSelected: (File?) -> Unit,
    modifier: Modifier = Modifier,
    accept: String? = null,
    helperText: String? = null
) {
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var previewBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isDragOver by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val interaction = remember { MutableInteractionSource() }
    val isHovered by interaction.collectIsHoveredAsState()
    val isPressed by interaction.collectIsPressedAsState()

    LaunchedEffect(selectedFile) {
        val file = selectedFile
        previewBitmap = if (file != null && isImageExtension(file.extension)) {
            withContext(Dispatchers.IO) {
                runCatching { ImageIO.read(file)?.toComposeImageBitmap() }.getOrNull()
            }
        } else null
    }

    val hasFile = selectedFile != null

    val borderColor by animateColorAsState(
        targetValue = when {
            isDragOver -> StudioColors.Violet500
            hasFile -> StudioColors.Zinc700
            isHovered -> StudioColors.Zinc600
            else -> StudioColors.Zinc800
        },
        animationSpec = StudioMotion.hoverSpec(),
        label = "file-input-border"
    )
    val bgColor by animateColorAsState(
        targetValue = when {
            isDragOver -> StudioColors.Violet500.copy(alpha = 0.1f)
            isHovered && !hasFile -> StudioColors.Zinc900.copy(alpha = 0.5f)
            else -> Color.Transparent
        },
        animationSpec = StudioMotion.hoverSpec(),
        label = "file-input-bg"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && !hasFile) 0.99f else 1f,
        animationSpec = StudioMotion.pressSpec(),
        label = "file-input-press"
    )
    val containerHeight by animateDpAsState(
        targetValue = if (hasFile) 96.dp else 112.dp,
        animationSpec = StudioMotion.collapseEnterSpec(),
        label = "file-input-height"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(containerHeight)
            .pressScale(pressScale)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .then(FileDropElement(
                onDragState = { isDragOver = it },
                onFilesDropped = { files ->
                    files.firstOrNull { accept == null || matchesAccept(it, accept) }?.let { picked ->
                        selectedFile = picked
                        onFileSelected(picked)
                    }
                }
            ))
            .hoverable(interaction)
            .pointerHoverIcon(if (hasFile) PointerIcon.Default else PointerIcon.Hand)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = !hasFile
            ) {
                scope.launch(Dispatchers.IO) {
                    val picked = openNativeFileDialog(promptText, accept) ?: return@launch
                    selectedFile = picked
                    onFileSelected(picked)
                }
            }
    ) {
        AnimatedContent(
            targetState = hasFile,
            transitionSpec = {
                (fadeIn(StudioMotion.collapseEnterSpec()) + scaleIn(StudioMotion.collapseEnterSpec(), initialScale = 0.94f))
                    .togetherWith(fadeOut(StudioMotion.popupExitSpec()) + scaleOut(StudioMotion.popupExitSpec(), targetScale = 0.94f))
            },
            label = "file-input-state"
        ) { fileShown ->
            val current = selectedFile
            if (fileShown && current != null) {
                SelectedFileRow(
                    file = current,
                    previewBitmap = previewBitmap,
                    onRemove = {
                        selectedFile = null
                        previewBitmap = null
                        onFileSelected(null)
                    }
                )
            } else {
                EmptyFileRow(promptText, helperText, isDragOver)
            }
        }
    }
}

@Composable
private fun EmptyFileRow(promptText: String, helperText: String?, isDragOver: Boolean) {
    val iconScale by animateFloatAsState(
        targetValue = if (isDragOver) 1.2f else 1f,
        animationSpec = StudioMotion.hoverSpec(),
        label = "file-input-icon-scale"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isDragOver) StudioColors.Violet500 else StudioColors.Zinc500,
        animationSpec = StudioMotion.hoverSpec(),
        label = "file-input-icon-color"
    )
    val promptColor by animateColorAsState(
        targetValue = if (isDragOver) StudioColors.Zinc100 else StudioColors.Zinc400,
        animationSpec = StudioMotion.hoverSpec(),
        label = "file-input-prompt-color"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .pressScale(iconScale)
        ) {
            SvgIcon(UPLOAD_ICON, 18.dp, iconColor)
        }
        Text(
            text = promptText,
            style = StudioTypography.medium(12),
            color = promptColor
        )
        if (helperText != null) {
            Text(
                text = helperText,
                style = StudioTypography.regular(11),
                color = StudioColors.Zinc600
            )
        }
    }
}

@Composable
private fun SelectedFileRow(
    file: File,
    previewBitmap: ImageBitmap?,
    onRemove: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(StudioColors.Zinc900)
                .border(1.dp, StudioColors.Zinc800, RoundedCornerShape(8.dp))
        ) {
            if (previewBitmap != null) {
                Image(
                    bitmap = previewBitmap,
                    contentDescription = file.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                SvgIcon(FILE_FALLBACK_ICON, 24.dp, StudioColors.Zinc500)
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = file.name,
                style = StudioTypography.semiBold(13),
                color = StudioColors.Zinc100,
                maxLines = 1
            )
            Text(
                text = formatFileSize(file.length()),
                style = StudioTypography.regular(11),
                color = StudioColors.Zinc500
            )
        }

        Spacer(Modifier.size(4.dp))
        RemoveButton(onRemove)
    }
}

@Composable
private fun RemoveButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()

    val bg by animateColorAsState(
        targetValue = if (hovered) StudioColors.Red400.copy(alpha = 0.15f) else Color.Transparent,
        animationSpec = StudioMotion.hoverSpec(),
        label = "file-remove-bg"
    )
    val tint by animateColorAsState(
        targetValue = if (hovered) StudioColors.Red400 else StudioColors.Zinc500,
        animationSpec = StudioMotion.hoverSpec(),
        label = "file-remove-tint"
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = StudioMotion.pressSpec(),
        label = "file-remove-scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(32.dp)
            .pressScale(scale)
            .clip(CircleShape)
            .background(bg)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interaction,
                indication = null
            ) { onClick() }
    ) {
        SvgIcon(CLOSE_ICON, 14.dp, tint)
    }
}

private fun matchesAccept(file: File, accept: String): Boolean {
    val trimmed = accept.trim()
    if (!trimmed.startsWith("*.")) return true
    return file.extension.equals(trimmed.removePrefix("*."), ignoreCase = true)
}

private fun isImageExtension(ext: String): Boolean = when (ext.lowercase()) {
    "png", "jpg", "jpeg", "gif", "bmp" -> true
    else -> false
}

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
}

private fun openNativeFileDialog(title: String, accept: String?): File? {
    val path = MemoryStack.stackPush().use { stack ->
        val filterPatterns = accept?.let { pattern ->
            stack.mallocPointer(1).apply {
                put(stack.UTF8(pattern))
                flip()
            }
        }
        TinyFileDialogs.tinyfd_openFileDialog(title, null, filterPatterns, accept, false)
    }
    return path?.let(::File)?.takeIf { it.isFile }
}

@OptIn(ExperimentalComposeUiApi::class)
private class FileDropElement(
    private val onDragState: (Boolean) -> Unit,
    private val onFilesDropped: (List<File>) -> Unit
) : ModifierNodeElement<FileDropNode>() {
    override fun create(): FileDropNode = FileDropNode(onDragState, onFilesDropped)

    override fun update(node: FileDropNode) {
        node.onDragState = onDragState
        node.onFilesDropped = onFilesDropped
    }

    override fun hashCode(): Int = onDragState.hashCode() * 31 + onFilesDropped.hashCode()

    override fun equals(other: Any?): Boolean = other is FileDropElement &&
        other.onDragState === onDragState &&
        other.onFilesDropped === onFilesDropped
}

@OptIn(ExperimentalComposeUiApi::class)
private class FileDropNode(
    var onDragState: (Boolean) -> Unit,
    var onFilesDropped: (List<File>) -> Unit
) : DelegatingNode() {
    init {
        delegate(DragAndDropTargetModifierNode(
            shouldStartDragAndDrop = { event -> event.dragData() is DragData.FilesList },
            target = object : DragAndDropTarget {
                override fun onDrop(event: DragAndDropEvent): Boolean {
                    val data = event.dragData() as? DragData.FilesList ?: return false
                    val files = data.readFiles().mapNotNull { uri ->
                        runCatching { File(URI(uri)) }.getOrNull()
                    }
                    onDragState(false)
                    if (files.isEmpty()) return false
                    onFilesDropped(files)
                    return true
                }

                override fun onEntered(event: DragAndDropEvent) { onDragState(true) }
                override fun onExited(event: DragAndDropEvent) { onDragState(false) }
                override fun onEnded(event: DragAndDropEvent) { onDragState(false) }
            }
        ))
    }
}

package fr.hardel.asset_editor.client.compose.components.codec.widget.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.google.gson.JsonElement
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.codec.CodecTokens
import fr.hardel.asset_editor.client.compose.components.codec.LocalCodecCategory
import fr.hardel.asset_editor.client.compose.components.codec.WidgetBody
import fr.hardel.asset_editor.client.compose.components.codec.WidgetHead
import fr.hardel.asset_editor.client.compose.components.codec.hasBody
import fr.hardel.asset_editor.client.compose.components.codec.hasHead
import fr.hardel.asset_editor.client.compose.components.codec.isSelfClearable
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.data.codec.CodecWidget
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

val FieldRowHeight: Dp = CodecTokens.RowHeight
val FieldRowRadius: Dp = CodecTokens.Radius
private val WARNING = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/warning.svg")

@Composable
fun FieldLabel(
    text: String,
    modifier: Modifier = Modifier,
    minWidth: Dp = CodecTokens.LabelMinWidth,
    color: Color = CodecTokens.Text
) {
    val category = LocalCodecCategory.current
    val shape = RoundedCornerShape(topStart = FieldRowRadius, bottomStart = FieldRowRadius)
    val bg = category?.labelBg ?: CodecTokens.LabelBg
    val borderColor = category?.border ?: CodecTokens.Border

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .height(FieldRowHeight)
            .widthIn(min = minWidth)
            .clip(shape)
            .background(bg, shape)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = CodecTokens.PaddingX)
    ) {
        Text(
            text = text,
            style = StudioTypography.medium(12),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun FieldRow(
    label: String,
    modifier: Modifier = Modifier,
    labelMinWidth: Dp = CodecTokens.LabelMinWidth,
    labelColor: Color = CodecTokens.Text,
    requiredMissing: Boolean = false,
    content: @Composable () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        modifier = modifier.fillMaxWidth().defaultMinSize(minHeight = FieldRowHeight)
    ) {
        FieldLabel(text = label, minWidth = labelMinWidth, color = labelColor)
        RequiredFieldFrame(requiredMissing = requiredMissing) { content() }
    }
}

@Composable
fun RequiredFieldFrame(
    requiredMissing: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (!requiredMissing) {
        Box(modifier = modifier) { content() }
        return
    }

    val shape = RoundedCornerShape(topEnd = FieldRowRadius, bottomEnd = FieldRowRadius)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CodecTokens.Remove.copy(alpha = 0.78f), shape)
    ) {
        content()
        RequiredWarningIcon(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp))
    }
}

@Composable
private fun RequiredWarningIcon(modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(CodecTokens.Remove.copy(alpha = 0.16f), CircleShape)
            .border(1.dp, CodecTokens.Remove.copy(alpha = 0.66f), CircleShape)
            .hoverable(interaction)
    ) {
        SvgIcon(WARNING, 14.dp, tint = CodecTokens.Error)
    }

    if (hovered) {
        Popup(alignment = Alignment.TopEnd, offset = IntOffset(0, -34)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(CodecTokens.RadiusLg))
                    .background(CodecTokens.PopupBg, RoundedCornerShape(CodecTokens.RadiusLg))
                    .border(1.dp, CodecTokens.Border, RoundedCornerShape(CodecTokens.RadiusLg))
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                Text(
                    text = I18n.get("codec:required"),
                    style = StudioTypography.regular(12),
                    color = CodecTokens.Text
                )
            }
        }
    }
}

@Composable
fun WithoutCodecCategory(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalCodecCategory provides null) { content() }
}

/**
 * Vertical container for nested children with an indent stripe on the left.
 * Mirrors the spyglass node-body layout: a 2px line at the indent center, with
 * children stacked beneath the parent row.
 */
@Composable
fun IndentBox(content: @Composable () -> Unit) {
    val borderColor = CodecTokens.IndentBorder
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = CodecTokens.Gap)
            .drawWithContent {
                val borderPx = CodecTokens.IndentBorderWidth.toPx()
                val xPos = (CodecTokens.IndentWidth.toPx() - borderPx) / 2f
                drawRect(
                    color = borderColor,
                    topLeft = Offset(xPos, 0f),
                    size = Size(borderPx, size.height)
                )
                drawContent()
            }
            .padding(start = CodecTokens.IndentWidth),
        verticalArrangement = Arrangement.spacedBy(CodecTokens.Gap)
    ) {
        content()
    }
}

/**
 * One field within a struct: `[Label][Head]` plus an indented body if the
 * widget has nested content. Handles optional add / remove buttons.
 */
@Composable
fun StructField(
    label: String,
    widget: CodecWidget,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    optional: Boolean,
    requiredMissing: Boolean = false,
    onAddOptional: (() -> Unit)? = null,
    onRemoveOptional: (() -> Unit)? = null
) {
    val absent = onAddOptional != null
    val labelColor = when {
        absent -> CodecTokens.TextMuted
        optional -> CodecTokens.TextDimmed
        else -> CodecTokens.Text
    }
    val selfClearable = onRemoveOptional != null && isSelfClearable(widget)
    val externalRemove = if (selfClearable) null else onRemoveOptional
    val inlineClear = if (selfClearable) onRemoveOptional else null

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            FieldLabel(text = label, color = labelColor)

            when {
                absent -> AddFieldButton(
                    label = I18n.get("codec:field.add"),
                    onClick = onAddOptional,
                    modifier = Modifier.weight(1f)
                )

                hasHead(widget) -> Box(modifier = Modifier.weight(1f)) {
                    RequiredFieldFrame(requiredMissing = requiredMissing) {
                        WidgetHead(widget, value, onValueChange, onClear = inlineClear)
                    }
                }

                else -> Spacer(Modifier.weight(1f))
            }

            if (externalRemove != null) {
                RemoveIconButton(onClick = externalRemove)
            }
        }

        if (!absent && hasBody(widget, value)) {
            IndentBox {
                WidgetBody(widget, value, onValueChange)
            }
        }
    }
}

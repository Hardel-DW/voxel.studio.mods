package fr.hardel.asset_editor.client.compose.components.layout.loading

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.AssetEditorClient
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import fr.hardel.asset_editor.client.compose.components.ui.GridBackground
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val LOGO = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/logo.svg")
private val GITHUB = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/company/github.svg")

private val DASHED_BORDER_COLOR = Color(0xFF151518)
private val CORNER_COLOR = Color(0xFF787878)
private val DOT_1_COLOR = Color(0xFF7A7A85)
private val DOT_2_COLOR = Color(0xFF404048)
private val DOT_3_COLOR = Color(0xFF1E1E21)

@Composable
fun Splash(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        GridBackground(modifier = Modifier.fillMaxSize().padding(top = 48.dp, start = 24.dp, end = 24.dp, bottom = 24.dp))

        CenterContent()

        LoadingText(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp))

        DashedFrame(modifier = Modifier.fillMaxSize().padding(top = 48.dp, start = 24.dp, end = 24.dp, bottom = 24.dp))

        TitleBar(modifier = Modifier.align(Alignment.TopStart))
    }
}

@Composable
private fun DashedFrame(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.drawBehind {
            val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
            drawRect(
                color = DASHED_BORDER_COLOR,
                style = Stroke(width = 1f, pathEffect = dash)
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            FrameTop(modifier = Modifier.align(Alignment.TopStart).fillMaxWidth())
            FrameBottom(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth())
        }

        CornerDecoration(Modifier.align(Alignment.TopStart).offset((-1).dp, (-1).dp), top = true, left = true)
        CornerDecoration(Modifier.align(Alignment.TopEnd).offset(1.dp, (-1).dp), top = true, left = false)
        CornerDecoration(Modifier.align(Alignment.BottomStart).offset((-1).dp, 1.dp), top = false, left = true)
        CornerDecoration(Modifier.align(Alignment.BottomEnd).offset(1.dp, 1.dp), top = false, left = false)
    }
}

@Composable
private fun CornerDecoration(modifier: Modifier, top: Boolean, left: Boolean) {
    val borderTop = if (top) 1.dp else 0.dp
    val borderBottom = if (!top) 1.dp else 0.dp
    val borderLeft = if (left) 1.dp else 0.dp
    val borderRight = if (!left) 1.dp else 0.dp

    Box(
        modifier = modifier
            .size(16.dp)
            .drawBehind {
                val c = CORNER_COLOR
                val w = size.width
                val h = size.height
                if (borderTop > 0.dp) drawLine(c, Offset(0f, 0f), Offset(w, 0f), strokeWidth = 1f)
                if (borderBottom > 0.dp) drawLine(c, Offset(0f, h), Offset(w, h), strokeWidth = 1f)
                if (borderLeft > 0.dp) drawLine(c, Offset(0f, 0f), Offset(0f, h), strokeWidth = 1f)
                if (borderRight > 0.dp) drawLine(c, Offset(w, 0f), Offset(w, h), strokeWidth = 1f)
            }
    )
}

@Composable
private fun FrameTop(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Dot(DOT_1_COLOR)
            Dot(DOT_2_COLOR)
            Dot(DOT_3_COLOR)
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = "BUILD ${AssetEditorClient.BUILD_VERSION}",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = VoxelColors.Zinc600,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun FrameBottom(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        val helpInteraction = remember { MutableInteractionSource() }
        val helpHovered by helpInteraction.collectIsHoveredAsState()

        Text(
            text = I18n.get("splash:help"),
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = if (helpHovered) VoxelColors.Zinc400 else VoxelColors.Zinc600,
            modifier = Modifier
                .hoverable(helpInteraction)
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable(interactionSource = helpInteraction, indication = null) {}
        )

        Spacer(Modifier.weight(1f))

        val ghInteraction = remember { MutableInteractionSource() }
        val ghHovered by ghInteraction.collectIsHoveredAsState()

        Box(
            modifier = Modifier
                .hoverable(ghInteraction)
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable(interactionSource = ghInteraction, indication = null) {}
                .alpha(if (ghHovered) 0.7f else 1.0f)
        ) {
            SvgIcon(location = GITHUB, size = 16.dp, tint = Color.White)
        }
    }
}

@Composable
private fun CenterContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        PulsingLogo()

        Spacer(Modifier.height(32.dp))

        BasicText(
            text = buildAnnotatedString {
                withStyle(SpanStyle(brush = Brush.verticalGradient(listOf(Color.White, VoxelColors.Zinc400)))) {
                    append(I18n.get("splash:title"))
                }
            },
            style = VoxelTypography.extraBold(36).copy(textAlign = TextAlign.Center)
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = I18n.get("splash:subtitle").uppercase(),
            style = VoxelTypography.medium(12),
            color = VoxelColors.Zinc500,
            letterSpacing = 3.6.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PulsingLogo() {
    val transition = rememberInfiniteTransition()
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse)
    )

    Box(modifier = Modifier.alpha(alpha)) {
        SvgIcon(location = LOGO, size = 96.dp, tint = Color.White)
    }
}

@Composable
private fun LoadingText(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition()
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse)
    )

    Text(
        text = I18n.get("splash:loading").uppercase(),
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        color = VoxelColors.Zinc400,
        letterSpacing = 1.sp,
        modifier = modifier.alpha(alpha)
    )
}

@Composable
private fun Dot(color: Color) {
    Box(modifier = Modifier.size(4.dp).background(color))
}

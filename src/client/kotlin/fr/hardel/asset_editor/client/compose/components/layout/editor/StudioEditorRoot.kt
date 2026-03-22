package fr.hardel.asset_editor.client.compose.components.layout.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.lib.StudioContext

private val contentShape = RoundedCornerShape(topStart = 24.dp)

@Composable
fun StudioEditorRoot(context: StudioContext, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxSize().background(VoxelColors.Sidebar)) {
        StudioPrimarySidebar(
            context = context,
            modifier = Modifier.width(64.dp).fillMaxHeight()
        )

        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            StudioEditorTabsBar(context = context)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .drawBehind {
                        val stroke = 1.25.dp.toPx()
                        val inset = 0.5.dp.toPx()
                        val safeWidth = (size.width - inset).coerceAtLeast(inset)
                        val safeHeight = (size.height - inset).coerceAtLeast(inset)
                        val radius = (24.dp.toPx() - inset).coerceAtMost(minOf(safeWidth, safeHeight))
                        val frame = Path().apply {
                            moveTo(inset + radius, inset)
                            quadraticTo(inset, inset, inset, inset + radius)
                            lineTo(inset, safeHeight)
                            moveTo(inset + radius, inset)
                            lineTo(safeWidth, inset)
                        }
                        drawPath(
                            path = frame,
                            color = VoxelColors.BorderAlpha50,
                            style = Stroke(width = stroke)
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(contentShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF18181D), VoxelColors.SurfaceHeader, VoxelColors.SurfaceHeader)
                            )
                        )
                ) {
                    ContentOutlet(context = context)
                }
            }
        }
    }
}

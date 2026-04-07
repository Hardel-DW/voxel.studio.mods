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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.lib.StudioContext

private val contentShape = RoundedCornerShape(topStart = 24.dp)

@Composable
fun StudioEditorRoot(context: StudioContext, modifier: Modifier = Modifier) {
    // div: flex h-dvh w-full overflow-hidden bg-sidebar
    Row(modifier = modifier.fillMaxSize().background(StudioColors.Sidebar)) {
        // aside: shrink-0 w-16 flex flex-col
        StudioPrimarySidebar(
            context = context,
            modifier = Modifier.width(64.dp).fillMaxHeight()
        )

        // div: flex-1 flex flex-col min-w-0
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            // header: shrink-0 h-12 select-none flex items-center gap-4 pl-4
            StudioEditorTabsBar(context = context)

            // main: flex-1 relative min-h-0 h-full bg-content overflow-hidden border-t border-l border-zinc-900 rounded-tl-3xl
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(contentShape)
                    .background(StudioColors.Content)
                    .drawWithContent {
                        drawContent()
                        val stroke = 1.dp.toPx()
                        val hs = stroke / 2f
                        val r = 24.dp.toPx()
                        val arcSize = Size((r - hs) * 2, (r - hs) * 2)
                        val borderColor = StudioColors.Zinc900

                        drawLine(borderColor, Offset(hs, r), Offset(hs, size.height), strokeWidth = stroke)
                        drawArc(borderColor, 180f, 90f, false, Offset(hs, hs), arcSize, style = Stroke(stroke))
                        drawLine(borderColor, Offset(r, hs), Offset(size.width, hs), strokeWidth = stroke)
                    }
            ) {
                ContentOutlet(context = context)
            }
        }
    }
}

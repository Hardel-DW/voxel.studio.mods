package fr.hardel.asset_editor.client.compose.components.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import fr.hardel.asset_editor.client.compose.lib.shortcut.StudioShortcutBus
import java.awt.event.KeyEvent
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
    DisposableEffect(context) {
        val handle = StudioShortcutBus.register { event ->
            if (event.id != KeyEvent.KEY_PRESSED) return@register false
            if (!event.isControlDown || event.keyCode != KeyEvent.VK_W) return@register false
            val activeTabId = context.navigationMemory().snapshot().activeTabId() ?: return@register false
            context.navigationMemory().closeTab(activeTabId)
            true
        }
        onDispose { handle.close() }
    }

    Row(modifier = modifier.fillMaxSize().background(StudioColors.Zinc925)) {
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
                    .clip(contentShape)
                    .background(StudioColors.Zinc800)
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

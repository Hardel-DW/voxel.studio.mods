package fr.hardel.asset_editor.client.compose.components.page.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.layout.EditorBreadcrumb
import fr.hardel.asset_editor.client.compose.components.layout.EditorHeaderTabItem
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.utils.ColorUtils
import net.minecraft.client.resources.language.I18n

private val DEBUG_TINT = ColorUtils.hueToColor(24, 0.74f, 0.58f)

@Composable
fun DebugLayout(context: StudioContext) {
    var currentTabId by remember { mutableStateOf(DebugTabRegistry.first()) }
    val currentTab = DebugTabRegistry.get(currentTabId) ?: return

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioColors.Zinc900.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(DEBUG_TINT.copy(alpha = 0.1f))
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                StudioColors.Zinc950.copy(alpha = 0.8f),
                                StudioColors.Zinc950
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, end = 32.dp, top = 32.dp, bottom = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        EditorBreadcrumb(
                            rootLabel = I18n.get("debug:layout.title"),
                            segments = emptyList(),
                            showBack = false,
                            onBack = null
                        )

                        Text(
                            text = I18n.get("debug:layout.title"),
                            style = StudioTypography.minecraftTen(36),
                            color = Color.White
                        )

                        Box(
                            modifier = Modifier
                                .padding(top = 3.dp)
                                .width(96.dp)
                                .height(1.dp)
                                .background(Brush.horizontalGradient(listOf(DEBUG_TINT, Color.Transparent)))
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .offset(y = 8.dp)
                ) {
                    DebugTabRegistry.all().forEach { tab ->
                        EditorHeaderTabItem(
                            label = I18n.get(tab.labelKey),
                            active = tab.id == currentTabId,
                            onClick = { currentTabId = tab.id }
                        )
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize().background(StudioColors.Zinc950)) {
            currentTab.render(context)
        }
    }
}

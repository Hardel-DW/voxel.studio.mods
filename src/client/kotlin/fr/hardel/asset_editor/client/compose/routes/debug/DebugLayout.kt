package fr.hardel.asset_editor.client.compose.routes.debug

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
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import fr.hardel.asset_editor.client.compose.components.layout.editor.EditorBreadcrumb
import fr.hardel.asset_editor.client.compose.components.layout.editor.EditorHeaderTabItem
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.utils.ColorUtils
import net.minecraft.client.resources.language.I18n

private const val TAB_WORKSPACE = "workspace"
private const val TAB_CODE = "code"
private const val TAB_RENDER = "render"
private const val TAB_LOGS = "logs"
private const val TAB_NETWORK = "network"
private val DEBUG_TINT = ColorUtils.hueToColor(24, 0.74f, 0.58f)

@Composable
fun DebugLayout(context: StudioContext) {
    var currentTab by remember { mutableStateOf(TAB_WORKSPACE) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(VoxelColors.Zinc900.copy(alpha = 0.5f))
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
                                VoxelColors.Zinc950.copy(alpha = 0.8f),
                                VoxelColors.Zinc950
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

                        androidx.compose.material.Text(
                            text = I18n.get("debug:layout.title"),
                            style = VoxelTypography.minecraftTen(36),
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
                    DebugTab(I18n.get("debug:layout.tab.workspace"), currentTab == TAB_WORKSPACE) { currentTab = TAB_WORKSPACE }
                    DebugTab(I18n.get("debug:layout.tab.code"), currentTab == TAB_CODE) { currentTab = TAB_CODE }
                    DebugTab(I18n.get("debug:layout.tab.render"), currentTab == TAB_RENDER) { currentTab = TAB_RENDER }
                    DebugTab(I18n.get("debug:layout.tab.logs"), currentTab == TAB_LOGS) { currentTab = TAB_LOGS }
                    DebugTab(I18n.get("debug:layout.tab.network"), currentTab == TAB_NETWORK) { currentTab = TAB_NETWORK }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize().background(VoxelColors.Zinc950)) {
            when (currentTab) {
                TAB_WORKSPACE -> DebugWorkspacePage(context)
                TAB_CODE -> DebugCodeBlockPage()
                TAB_LOGS -> DebugLogsPage(context)
                TAB_NETWORK -> DebugNetworkPage(context)
                else -> DebugRenderPage()
            }
        }
    }
}

@Composable
private fun DebugTab(label: String, active: Boolean, onClick: () -> Unit) {
    EditorHeaderTabItem(
        label = label,
        active = active,
        onClick = onClick
    )
}

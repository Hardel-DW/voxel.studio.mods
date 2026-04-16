package fr.hardel.asset_editor.client.compose.components.page.debug

import androidx.compose.ui.graphics.Color
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.memory.session.debug.DebugLogMemory

fun debugLogLevelColor(level: DebugLogMemory.Level): Color = when (level) {
    DebugLogMemory.Level.INFO -> StudioColors.Sky400
    DebugLogMemory.Level.WARN -> StudioColors.Amber400
    DebugLogMemory.Level.ERROR -> StudioColors.Red400
    DebugLogMemory.Level.SUCCESS -> StudioColors.Zinc100
}

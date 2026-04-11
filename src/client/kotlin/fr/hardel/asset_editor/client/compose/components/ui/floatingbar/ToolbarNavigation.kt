package fr.hardel.asset_editor.client.compose.components.ui.floatingbar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import net.minecraft.resources.Identifier

private val ARROW_LEFT = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/arrow-left.svg")
private val ARROW_RIGHT = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/arrow-right.svg")

// TSX: flex items-center gap-1 (4dp), buttons size-8 (32dp), rounded-lg (8dp), hover:bg-white/10
@Composable
fun ToolbarNavigation(
    canGoBack: Boolean = false,
    canGoForward: Boolean = false,
    onBack: () -> Unit = {},
    onForward: () -> Unit = {}
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        NavButton(icon = ARROW_LEFT, onClick = onBack, enabled = canGoBack)
        NavButton(icon = ARROW_RIGHT, onClick = onForward, enabled = canGoForward)
    }
}

@Composable
private fun NavButton(icon: Identifier, onClick: () -> Unit, enabled: Boolean) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(32.dp) // size-8
            .hoverable(interaction)
            .background(
                if (hovered && enabled) Color.White.copy(alpha = 0.1f) else Color.Transparent, // hover:bg-white/10
                RoundedCornerShape(8.dp) // rounded-lg
            )
            .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
            .then(
                if (enabled) Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onClick() }
                else Modifier
            )
    ) {
        SvgIcon(
            icon, 16.dp, // size-4
            Color.White.copy(alpha = if (enabled) 0.7f else 0.3f) // opacity-70 / disabled:opacity-30
        )
    }
}

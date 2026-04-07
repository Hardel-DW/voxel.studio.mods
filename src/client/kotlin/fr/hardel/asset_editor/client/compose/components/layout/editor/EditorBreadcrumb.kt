package fr.hardel.asset_editor.client.compose.components.layout.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier
import java.util.Locale

private val BACK_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/back.svg")

@Composable
fun EditorBreadcrumb(
    rootLabel: String,
    segments: List<String>,
    showBack: Boolean,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    // div: flex items-center gap-2 text-zinc-400 text-sm mb-1
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.padding(bottom = 4.dp)
    ) {
        if (showBack && onBack != null) {
            // button: cursor-pointer flex items-center gap-1.5 text-zinc-400 hover:text-white transition-colors mr-2
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .padding(end = 8.dp)
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBack
                    )
            ) {
                SvgIcon(BACK_ICON, 14.dp, StudioColors.Zinc400, modifier = Modifier.alpha(0.5f))
                Text(
                    text = I18n.get("generic:back"),
                    style = StudioTypography.medium(12),
                    color = StudioColors.Zinc400
                )
            }
        }

        Text(
            text = rootLabel.uppercase(Locale.ROOT),
            style = StudioTypography.medium(12),
            color = StudioColors.Zinc400,
            modifier = Modifier.alpha(0.5f)
        )

        segments.forEachIndexed { index, segment ->
            // span: flex items-center gap-2
            Text(
                text = "/",
                style = StudioTypography.regular(12),
                color = StudioColors.Zinc500,
                modifier = Modifier.alpha(0.3f)
            )

            if (index == segments.lastIndex) {
                // span: text-zinc-200 font-medium text-xs uppercase tracking-wider bg-black/20 px-2 py-0.5 rounded-md border border-white/5
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = segment.uppercase(Locale.ROOT),
                        style = StudioTypography.medium(12),
                        color = StudioColors.Zinc300
                    )
                }
            } else {
                // span: opacity-50 font-medium text-xs uppercase tracking-wider
                Text(
                    text = segment.uppercase(Locale.ROOT),
                    style = StudioTypography.medium(12),
                    color = StudioColors.Zinc400,
                    modifier = Modifier.alpha(0.5f)
                )
            }
        }
    }
}

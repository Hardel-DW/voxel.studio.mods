package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import net.minecraft.resources.Identifier

private val CHECK_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/check.svg")
private val LOCK_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/tools/lock.svg")

@Composable
fun InlineCard(
    title: String,
    description: String,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    locked: Boolean = false,
    lockText: String? = null
) {
    val descText = if (locked && lockText != null) lockText else description

    SimpleCard(
        padding = PaddingValues(vertical = 16.dp, horizontal = 24.dp),
        active = active,
        onClick = if (!locked) ({ onActiveChange(!active) }) else null,
        modifier = modifier.alpha(if (locked) 0.5f else 1f)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = StudioTypography.regular(16),
                    color = Color.White
                )
                Text(
                    text = descText,
                    style = StudioTypography.light(12),
                    color = StudioColors.Zinc400
                )
            }

            if (locked) {
                SvgIcon(LOCK_ICON, 24.dp, Color.White)
            } else if (active) {
                SvgIcon(CHECK_ICON, 24.dp, Color.White)
            }
        }
    }
}

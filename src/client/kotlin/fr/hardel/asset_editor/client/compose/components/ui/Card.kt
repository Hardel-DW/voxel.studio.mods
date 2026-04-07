package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
fun Card(
    imageId: Identifier,
    title: String,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    locked: Boolean = false,
    lockText: String? = null
) {
    SimpleCard(
        padding = PaddingValues(16.dp),
        active = active,
        onClick = if (!locked) ({ onActiveChange(!active) }) else null,
        modifier = modifier.alpha(if (locked) 0.5f else 1f),
        overlay = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (locked) {
                    SvgIcon(LOCK_ICON, 24.dp, Color.White, Modifier.align(Alignment.TopEnd))
                    if (lockText != null) {
                        Text(
                            text = lockText,
                            style = StudioTypography.light(11),
                            color = StudioColors.Zinc400,
                            modifier = Modifier.align(Alignment.BottomEnd)
                        )
                    }
                } else if (active) {
                    SvgIcon(CHECK_ICON, 24.dp, Color.White, Modifier.align(Alignment.TopEnd))
                }
            }
        }
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = title,
                    style = StudioTypography.semiBold(16),
                    color = StudioColors.Zinc100
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = StudioTypography.regular(13),
                        color = StudioColors.Zinc400
                    )
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(bottom = 32.dp)
                    .size(64.dp)
            ) {
                ResourceImageIcon(
                    location = imageId,
                    size = 64.dp,
                    modifier = Modifier.size(64.dp)
                )
            }
        }
    }
}

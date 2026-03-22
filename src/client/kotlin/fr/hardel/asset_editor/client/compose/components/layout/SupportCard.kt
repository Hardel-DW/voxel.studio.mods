package fr.hardel.asset_editor.client.compose.components.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import fr.hardel.asset_editor.client.compose.components.ui.Button
import fr.hardel.asset_editor.client.compose.components.ui.ButtonSize
import fr.hardel.asset_editor.client.compose.components.ui.ButtonVariant
import fr.hardel.asset_editor.client.compose.components.ui.ShineOverlay
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.lib.utils.BrowserUtils
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val LOGO = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/logo.svg")
private val CHECK = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/check.svg")
private val PATREON_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/company/patreon.svg")
private val ADVANTAGES = listOf("early_access", "submit_ideas", "discord_role", "live_voxel")
private val supportCardShape = RoundedCornerShape(16.dp)

@Composable
fun SupportCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(0xFF0B0B0B), supportCardShape)
            .border(2.dp, Color(0xFF1C1917), supportCardShape)
    ) {
        ShineOverlay(
            modifier = Modifier.matchParentSize(),
            opacity = 0.15f
        )

        SvgIcon(
            location = LOGO,
            size = 384.dp,
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 96.dp, y = (-96).dp)
                .alpha(0.2f)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 48.dp, end = 32.dp, top = 32.dp, bottom = 32.dp)
        ) {
            Text(
                text = I18n.get("supports:title"),
                style = VoxelTypography.semiBold(30),
                color = Color.White
            )
            Text(
                text = I18n.get("supports:description"),
                style = VoxelTypography.regular(14),
                color = VoxelColors.Zinc400,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .widthIn(max = 480.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = I18n.get("supports:advantages"),
                        style = VoxelTypography.bold(20),
                        color = Color.White,
                        modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            ADVANTAGES.filterIndexed { index, _ -> index % 2 == 0 }.forEach { advantage ->
                                SupportAdvantage("supports:advantages.$advantage")
                            }
                        }
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            ADVANTAGES.filterIndexed { index, _ -> index % 2 == 1 }.forEach { advantage ->
                                SupportAdvantage("supports:advantages.$advantage")
                            }
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 32.dp)
                ) {
                    Button(
                        onClick = { BrowserUtils.openBrowser("https://streamelements.com/hardoudou/tip") },
                        variant = ButtonVariant.SHIMMER,
                        size = ButtonSize.LG,
                        text = I18n.get("donate")
                    )
                    Button(
                        onClick = { BrowserUtils.openBrowser("https://www.patreon.com/hardel") },
                        variant = ButtonVariant.PATREON,
                        size = ButtonSize.LG,
                        text = I18n.get("supports:become"),
                        icon = {
                            SvgIcon(PATREON_ICON, 16.dp, Color.White)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SupportAdvantage(key: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SvgIcon(CHECK, 16.dp, Color.White)
        Text(
            text = I18n.get(key),
            style = VoxelTypography.semiBold(14),
            color = VoxelColors.Zinc300
        )
    }
}

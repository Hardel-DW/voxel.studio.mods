package fr.hardel.asset_editor.client.compose.components.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
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
private val cardShape = RoundedCornerShape(16.dp)
private val borderColor = StudioColors.Stone900

@Composable
fun SupportCard(modifier: Modifier = Modifier) {
    // bg-black/35 border-t-2 border-l-2 rounded-2xl border-stone-900 overflow-hidden relative
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(Color.Black.copy(alpha = 0.35f), cardShape)
            .drawWithContent {
                drawContent()
                val stroke = 2.dp.toPx()
                val hs = stroke / 2f
                val r = 16.dp.toPx()
                val arcSize = Size((r - hs) * 2, (r - hs) * 2)

                drawLine(borderColor, Offset(hs, size.height), Offset(hs, r), strokeWidth = stroke)
                drawArc(borderColor, 180f, 90f, false, Offset(hs, hs), arcSize, style = Stroke(stroke))
                drawLine(borderColor, Offset(r, hs), Offset(size.width, hs), strokeWidth = stroke)
            }
    ) {
        // Background shine
        ShineOverlay(modifier = Modifier.matchParentSize(), opacity = 0.15f)

        // absolute -top-24 -right-24 size-96 opacity-20
        // matchParentSize so the icon doesn't inflate the Box height (CSS: position absolute)
        Box(
            contentAlignment = Alignment.TopEnd,
            modifier = Modifier.matchParentSize()
        ) {
            SvgIcon(
                location = LOGO,
                size = 384.dp,
                tint = Color.White,
                modifier = Modifier
                    .offset(x = 96.dp, y = (-96).dp)
                    .alpha(0.2f)
            )
        }

        // flex flex-col justify-between h-full p-8 pl-12
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 48.dp, end = 32.dp, top = 32.dp, bottom = 32.dp)
        ) {
            // Title + description
            Text(
                text = I18n.get("supports:title"),
                style = StudioTypography.semiBold(30),
                color = Color.White
            )
            Text(
                text = I18n.get("supports:description"),
                style = StudioTypography.regular(14),
                color = StudioColors.Zinc400,
                modifier = Modifier.padding(top = 8.dp)
            )

            // xl:flex justify-between gap-4 mt-4
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                // Advantages column
                Column(modifier = Modifier.weight(1f)) {
                    // h3: text-white font-bold text-xl pb-4 pt-6
                    Text(
                        text = I18n.get("supports:advantages"),
                        style = StudioTypography.bold(20),
                        color = Color.White,
                        modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
                    )

                    // grid grid-cols-2 gap-x-8 gap-y-4
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            ADVANTAGES.filterIndexed { i, _ -> i % 2 == 0 }.forEach {
                                AdvantageItem("supports:advantages.$it")
                            }
                        }
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            ADVANTAGES.filterIndexed { i, _ -> i % 2 == 1 }.forEach {
                                AdvantageItem("supports:advantages.$it")
                            }
                        }
                    }
                }

                // Buttons: self-end gap-4 pt-8
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.align(Alignment.Bottom).padding(top = 32.dp)
                ) {
                    Button(
                        onClick = { BrowserUtils.openBrowser("https://streamelements.com/hardoudou/tip") },
                        variant = ButtonVariant.SHIMMER,
                        size = ButtonSize.LG,
                        text = I18n.get("supports:donate")
                    )
                    Button(
                        onClick = { BrowserUtils.openBrowser("https://www.patreon.com/hardel") },
                        variant = ButtonVariant.PATREON,
                        size = ButtonSize.LG,
                        text = I18n.get("supports:become"),
                        icon = { SvgIcon(PATREON_ICON, 16.dp, Color.White) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdvantageItem(key: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SvgIcon(CHECK, 16.dp, Color.White)
        Text(
            text = I18n.get(key),
            style = StudioTypography.semiBold(14),
            color = StudioColors.Zinc300
        )
    }
}

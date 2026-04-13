package fr.hardel.asset_editor.client.compose.components.layout.loading

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.window.LocalWindowChromeState
import fr.hardel.asset_editor.client.compose.window.WindowChromeDefaults
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val LOGO = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/logo.svg")

@Composable
fun TitleBar(modifier: Modifier = Modifier) {
    val chromeState = LocalWindowChromeState.current
    val density = LocalDensity.current
    val leftInset = with(density) { chromeState.leftInsetPx.toDp() }
    val rightInset = with(density) { chromeState.rightInsetPx.toDp() }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(WindowChromeDefaults.SPLASH_TITLE_BAR_HEIGHT_DP.dp)
                .padding(start = 12.dp + leftInset, end = 12.dp + rightInset)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                SvgIcon(location = LOGO, size = 16.dp, tint = Color.White)
                Text(
                    text = I18n.get("app:title"),
                    style = StudioTypography.medium(12),
                    color = StudioColors.Zinc400,
                    modifier = Modifier.padding(start = 8.dp)
                )

                Spacer(Modifier.weight(1f))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(StudioColors.Editor, StudioColors.HeaderCloudy, StudioColors.Editor)
                    )
                )
        )
    }
}

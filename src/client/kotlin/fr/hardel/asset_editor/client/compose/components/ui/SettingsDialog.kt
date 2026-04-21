package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.bootstrap.ComposeBootstrap
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.lib.selectAsFlow
import fr.hardel.asset_editor.client.compose.window.VoxelStudioWindow
import fr.hardel.asset_editor.client.memory.ClientMemoryHolder
import net.minecraft.client.resources.language.I18n

/**
 * Persistent user-settings dialog. All values are backed by [ClientMemoryHolder.settings] and
 * written to `config/asset_editor.json` on every toggle.
 *
 * Adding a new option: drop another [SettingRow] or [SettingActionRow] in the relevant
 * [SettingsSection]. Adding a new category: add a [SettingsSection] block to the Column
 * below. The dialog scrolls when content overflows so sections can grow freely.
 */
@Composable
fun SettingsDialog(onDismiss: () -> Unit) {
    val settings = ClientMemoryHolder.settings()
    val snapshot by settings.selectAsFlow { it }.collectAsState(initial = settings.snapshot())

    Dialog(title = I18n.get("settings:title"), onDismiss = onDismiss) {
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = I18n.get("settings:description"),
                style = StudioTypography.regular(12),
                color = StudioColors.Zinc400
            )

            SettingsSection(titleKey = "settings:performance.title") {
                SettingRow(
                    titleKey = "settings:performance.fps_counter.title",
                    descriptionKey = "settings:performance.fps_counter.description"
                ) {
                    ToggleSwitch(
                        checked = snapshot.showFpsCounter,
                        onCheckedChange = settings::setShowFpsCounter
                    )
                }
                SettingRow(
                    titleKey = "settings:performance.disable_vsync.title",
                    descriptionKey = "settings:performance.disable_vsync.description",
                    footnoteKey = "settings:performance.disable_vsync.restart_required"
                ) {
                    ToggleSwitch(
                        checked = snapshot.disableVsync,
                        onCheckedChange = settings::setDisableVsync
                    )
                }
            }

            SettingsSection(titleKey = "settings:debug.title") {
                SettingRow(
                    titleKey = "settings:debug.stay_on_splash.title",
                    descriptionKey = "settings:debug.stay_on_splash.description"
                ) {
                    ToggleSwitch(
                        checked = snapshot.stayOnSplash,
                        onCheckedChange = settings::setStayOnSplash
                    )
                }
                SettingRow(
                    titleKey = "settings:debug.show_hover_triangle.title",
                    descriptionKey = "settings:debug.show_hover_triangle.description"
                ) {
                    ToggleSwitch(
                        checked = snapshot.showHoverTriangle,
                        onCheckedChange = settings::setShowHoverTriangle
                    )
                }
                SettingActionRow(
                    titleKey = "settings:debug.clear_compose_cache.title",
                    descriptionKey = "settings:debug.clear_compose_cache.description",
                    buttonTextKey = "settings:debug.clear_compose_cache.button"
                ) {
                    ComposeBootstrap.purgeCache()
                    VoxelStudioWindow.requestClose()
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    titleKey: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = I18n.get(titleKey).uppercase(),
            style = StudioTypography.semiBold(10),
            color = StudioColors.Zinc500,
            modifier = Modifier.padding(start = 4.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingRow(
    titleKey: String,
    descriptionKey: String,
    footnoteKey: String? = null,
    control: @Composable () -> Unit
) {
    SettingRowContainer {
        SettingRowLabels(titleKey, descriptionKey, footnoteKey, modifier = Modifier.weight(1f))
        Spacer(Modifier.padding(start = 16.dp))
        control()
    }
}

@Composable
private fun SettingActionRow(
    titleKey: String,
    descriptionKey: String,
    buttonTextKey: String,
    onClick: () -> Unit
) {
    SettingRowContainer {
        SettingRowLabels(titleKey, descriptionKey, footnoteKey = null, modifier = Modifier.weight(1f))
        Spacer(Modifier.padding(start = 16.dp))
        Button(
            onClick = onClick,
            variant = ButtonVariant.GHOST_BORDER,
            size = ButtonSize.SM,
            text = I18n.get(buttonTextKey)
        )
    }
}

@Composable
private fun SettingRowContainer(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(StudioColors.Zinc900.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .border(1.dp, StudioColors.Zinc800, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        content = content
    )
}

@Composable
private fun SettingRowLabels(
    titleKey: String,
    descriptionKey: String,
    footnoteKey: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = I18n.get(titleKey),
            style = StudioTypography.medium(13),
            color = StudioColors.Zinc200
        )
        Text(
            text = I18n.get(descriptionKey),
            style = StudioTypography.regular(11),
            color = StudioColors.Zinc500
        )
        if (footnoteKey != null) {
            Text(
                text = I18n.get(footnoteKey),
                style = StudioTypography.regular(10),
                color = StudioColors.Zinc600
            )
        }
    }
}

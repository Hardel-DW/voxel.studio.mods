package fr.hardel.asset_editor.client.compose.components.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.Button
import fr.hardel.asset_editor.client.compose.components.ui.ButtonSize
import fr.hardel.asset_editor.client.compose.components.ui.ButtonVariant
import fr.hardel.asset_editor.client.compose.components.ui.Dialog
import fr.hardel.asset_editor.client.compose.components.ui.FileInput
import fr.hardel.asset_editor.client.compose.components.ui.InputText
import fr.hardel.asset_editor.client.network.ClientPayloadSender
import fr.hardel.asset_editor.network.pack.PackCreatePayload
import java.io.File
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

object PackCreateDialog {

    @Composable
    fun create(
        onDismiss: () -> Unit
    ) {
        var name by remember { mutableStateOf("") }
        var namespace by remember { mutableStateOf("") }
        var syncing by remember { mutableStateOf(false) }
        var userEditedNamespace by remember { mutableStateOf(false) }
        var iconFile by remember { mutableStateOf<File?>(null) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        Dialog(
            title = I18n.get("studio:pack.create.title"),
            onDismiss = onDismiss,
            footer = {
                Button(
                    onClick = onDismiss,
                    variant = ButtonVariant.GHOST_BORDER,
                    size = ButtonSize.SM,
                    text = I18n.get("studio:action.cancel")
                )
                Button(
                    onClick = {
                        val nextName = name.trim()
                        val nextNamespace = namespace.trim()
                        val iconBytes = iconFile?.takeIf { it.isFile }?.readBytes() ?: ByteArray(0)
                        errorMessage = when {
                            nextName.isEmpty() || nextNamespace.isEmpty() ->
                                I18n.get("error:pack_name_and_namespace_required")
                            !Identifier.isValidNamespace(nextNamespace) ->
                                I18n.get("error:invalid_namespace")
                            iconBytes.size > PackCreatePayload.MAX_ICON_BYTES ->
                                I18n.get("error:pack_icon_too_large")
                            else -> null
                        }
                        if (errorMessage == null) {
                            ClientPayloadSender.send(PackCreatePayload(nextName, nextNamespace, iconBytes))
                            onDismiss()
                        }
                    },
                    variant = ButtonVariant.SHIMMER,
                    size = ButtonSize.SM,
                    text = I18n.get("studio:action.create")
                )
            }
        ) {
            // div: flex flex-col gap-3
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                FieldLabel("studio:pack.create.name")
                InputText(
                    value = name,
                    onValueChange = { value ->
                        name = value
                        errorMessage = null
                        if (!userEditedNamespace) {
                            syncing = true
                            namespace = value.lowercase().replace(Regex("[^a-z0-9_]"), "_")
                            syncing = false
                        }
                    },
                    placeholder = I18n.get("studio:pack.create.name.placeholder")
                )

                FieldLabel("studio:pack.create.namespace")
                InputText(
                    value = namespace,
                    onValueChange = { value ->
                        namespace = value
                        errorMessage = null
                        if (!syncing) {
                            userEditedNamespace = true
                        }
                    },
                    placeholder = I18n.get("studio:pack.create.namespace.placeholder")
                )

                FieldLabel("studio:pack.create.icon")
                FileInput(
                    promptText = I18n.get("studio:pack.create.icon.placeholder"),
                    helperText = I18n.get("studio:pack.create.icon.hint"),
                    accept = "*.png",
                    onFileSelected = { file ->
                        iconFile = file?.takeIf { it.extension.equals("png", ignoreCase = true) }
                        errorMessage = when {
                            file == null -> null
                            iconFile == null -> I18n.get("error:pack_icon_not_png")
                            else -> null
                        }
                    }
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        style = StudioTypography.regular(12),
                        color = StudioColors.Red400
                    )
                }
            }
        }
    }

    @Composable
    private fun FieldLabel(key: String) {
        Text(
            text = I18n.get(key),
            style = StudioTypography.medium(12),
            color = StudioColors.Zinc300
        )
    }
}

package fr.hardel.asset_editor.client.compose.lib

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import fr.hardel.asset_editor.client.compose.components.layout.editor.PackCreateDialog
import fr.hardel.asset_editor.client.compose.components.ui.Button
import fr.hardel.asset_editor.client.compose.components.ui.ButtonSize
import fr.hardel.asset_editor.client.compose.components.ui.ButtonVariant
import fr.hardel.asset_editor.client.compose.components.ui.Dialog
import fr.hardel.asset_editor.client.compose.lib.action.EditorActionResult
import fr.hardel.asset_editor.store.ElementEntry
import fr.hardel.asset_editor.workspace.action.EditorAction
import net.minecraft.client.resources.language.I18n
import net.minecraft.core.Registry
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey

class RegistryDialogState {
    var packRequired by mutableStateOf(false)
    var errorKey by mutableStateOf<String?>(null)
    var createPack by mutableStateOf(false)

    fun handle(result: EditorActionResult) {
        when (result.status) {
            EditorActionResult.Status.APPLIED -> Unit
            EditorActionResult.Status.PACK_REQUIRED -> packRequired = true
            EditorActionResult.Status.REJECTED,
            EditorActionResult.Status.ERROR -> errorKey = result.message ?: "error:unknown"
        }
    }
}

@Composable
fun rememberRegistryDialogState(): RegistryDialogState =
    remember { RegistryDialogState() }

@Composable
fun <T : Any> rememberRegistryVersion(
    context: StudioContext,
    registry: ResourceKey<Registry<T>>
): Int {
    var version by remember(context, registry) { mutableIntStateOf(0) }

    DisposableEffect(context, registry) {
        val listener = Runnable { version++ }
        context.elementStore().subscribeRegistry(registry, listener)
        onDispose {
            context.elementStore().unsubscribeRegistry(registry, listener)
        }
    }

    return version
}

@Composable
fun <T : Any> rememberRegistryEntries(
    context: StudioContext,
    registry: ResourceKey<Registry<T>>
): List<ElementEntry<T>> {
    val version = rememberRegistryVersion(context, registry)
    return remember(context, registry, version) {
        context.allTypedEntries(registry)
    }
}

@Composable
fun <T : Any> rememberCurrentRegistryEntry(
    context: StudioContext,
    registry: ResourceKey<Registry<T>>
): ElementEntry<T>? {
    val version = rememberRegistryVersion(context, registry)
    return remember(context, registry, context.currentElementId, version) {
        context.currentEntry(registry)
    }
}

fun StudioContext.hasWritablePack(): Boolean =
    selectedPack?.writable() == true

fun <T : Any> StudioContext.dispatchRegistryAction(
    registry: ResourceKey<Registry<T>>,
    target: Identifier?,
    action: EditorAction,
    dialogs: RegistryDialogState
): EditorActionResult {
    val result = gateway.dispatch(registry, target, action)
    dialogs.handle(result)
    return result
}

@Composable
fun RegistryPageDialogs(
    context: StudioContext,
    state: RegistryDialogState
) {
    val errorKey = state.errorKey

    if (state.packRequired) {
        Dialog(
            title = I18n.get("studio:pack.required.title"),
            onDismiss = { state.packRequired = false },
            footer = {
                Button(
                    onClick = { state.packRequired = false },
                    variant = ButtonVariant.GHOST_BORDER,
                    size = ButtonSize.SM,
                    text = I18n.get("studio:action.cancel")
                )
                Button(
                    onClick = {
                        state.packRequired = false
                        state.createPack = true
                    },
                    variant = ButtonVariant.SHIMMER,
                    size = ButtonSize.SM,
                    text = I18n.get("studio:pack.create")
                )
            }
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                androidx.compose.material.Text(
                    text = I18n.get("studio:pack.required.message"),
                    style = VoxelTypography.regular(13),
                    color = VoxelColors.Zinc400
                )
            }
        }
    }

    if (errorKey != null) {
        Dialog(
            title = I18n.get("error:dialog.title"),
            onDismiss = { state.errorKey = null },
            footer = {
                Button(
                    onClick = { state.errorKey = null },
                    variant = ButtonVariant.GHOST_BORDER,
                    size = ButtonSize.SM,
                    text = I18n.get("studio:action.cancel")
                )
            }
        ) {
            androidx.compose.material.Text(
                text = I18n.get(errorKey),
                style = VoxelTypography.regular(13),
                color = VoxelColors.Zinc400
            )
        }
    }

    if (state.createPack) {
        PackCreateDialog.create(context) {
            state.createPack = false
        }
    }
}

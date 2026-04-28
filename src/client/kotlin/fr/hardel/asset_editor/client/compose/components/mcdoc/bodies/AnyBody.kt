package fr.hardel.asset_editor.client.compose.components.mcdoc.bodies

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.gson.JsonElement

@Composable
fun AnyBody(
    @Suppress("UNUSED_PARAMETER") value: JsonElement?,
    @Suppress("UNUSED_PARAMETER") onValueChange: (JsonElement) -> Unit,
    @Suppress("UNUSED_PARAMETER") modifier: Modifier = Modifier
) {
    // Any/Unsafe types are edited as raw JSON via AnyHead. No nested body.
}

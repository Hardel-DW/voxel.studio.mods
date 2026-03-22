package fr.hardel.asset_editor.client.compose.components.page.changes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.hardel.asset_editor.client.compose.routes.changes.ChangesMainPage

@Composable
fun ChangesLayout(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        ChangesMainPage()
    }
}

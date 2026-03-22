package fr.hardel.asset_editor.client.compose.window

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import fr.hardel.asset_editor.client.AssetEditorClient
import fr.hardel.asset_editor.client.compose.components.layout.editor.StudioEditorRoot
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.data.StudioConcept
import fr.hardel.asset_editor.client.compose.components.layout.loading.Splash
import kotlinx.coroutines.delay

@Composable
fun ComposeStudioContent(windowState: WindowState) {
    val context = remember {
        StudioContext(
            AssetEditorClient.sessionState(),
            AssetEditorClient.sessionDispatch()
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            context.dispose()
        }
    }

    LaunchedEffect(Unit) {
        delay(1000)
        windowState.markSplashTimerDone()
    }

    LaunchedEffect(Unit) {
        while (true) {
            if (AssetEditorClient.sessionState().hasReceivedPermissions()) {
                windowState.markPermissionsReceived()
                break
            }
            delay(100)
        }
    }

    LaunchedEffect(windowState.phase) {
        if (windowState.phase == WindowPhase.EDITOR) {
            context.resyncWorldSession()
            context.router.revalidate()
            if (context.router.currentRoute == fr.hardel.asset_editor.client.compose.routes.StudioRoute.NoPermission) {
                StudioConcept.firstAccessible(AssetEditorClient.sessionState().permissions())?.let { concept ->
                    context.router.navigate(concept.overviewRoute)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when (windowState.phase) {
            WindowPhase.SPLASH -> Splash()
            WindowPhase.EDITOR -> StudioEditorRoot(context = context)
        }
    }
}

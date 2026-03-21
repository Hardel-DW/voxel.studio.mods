package fr.hardel.asset_editor.client.compose.window

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import fr.hardel.asset_editor.client.AssetEditorClient
import fr.hardel.asset_editor.client.compose.components.layout.editor.StudioEditorRoot
import fr.hardel.asset_editor.client.compose.components.layout.loading.Splash
import fr.hardel.asset_editor.client.compose.routes.StudioRoute
import fr.hardel.asset_editor.client.compose.routes.StudioRouter
import kotlinx.coroutines.delay

@Composable
fun ComposeStudioContent(windowState: WindowState) {
    val router = remember { StudioRouter() }

    LaunchedEffect(Unit) {
        delay(30_000)
        windowState.markSplashTimerDone()
    }

    LaunchedEffect(Unit) {
        while (true) {
            if (AssetEditorClient.sessionState().hasReceivedPermissions()) {
                router.permissionSupplier = { AssetEditorClient.sessionState().permissions() }
                windowState.markPermissionsReceived()
                break
            }
            delay(100)
        }
    }

    LaunchedEffect(windowState.phase) {
        if (windowState.phase == WindowPhase.EDITOR) {
            router.revalidate()
            if (router.currentRoute is StudioRoute.NoPermission) {
                val perm = AssetEditorClient.sessionState().permissions()
                if (!perm.isNone) {
                    router.navigate(StudioRoute.EnchantmentOverview)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when (windowState.phase) {
            WindowPhase.SPLASH -> Splash()
            WindowPhase.EDITOR -> StudioEditorRoot(router = router)
        }
    }
}

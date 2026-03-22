package fr.hardel.asset_editor.client.compose.window

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import fr.hardel.asset_editor.client.AssetEditorClient
import fr.hardel.asset_editor.client.compose.components.layout.editor.StudioEditorRoot
import fr.hardel.asset_editor.client.compose.components.layout.loading.Splash
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.assets.LocalStudioAssetCache
import fr.hardel.asset_editor.client.compose.lib.data.StudioConcept
import fr.hardel.asset_editor.client.navigation.NoPermissionDestination
import fr.hardel.asset_editor.client.selector.Subscription
import java.awt.Component
import javax.swing.SwingUtilities
import kotlinx.coroutines.delay
import net.minecraft.client.resources.language.I18n

object VoxelStudioWindow : MinecraftStageWindow(680, 440) {

    private enum class State { SPLASH, EDITOR }

    private var state by mutableStateOf(State.SPLASH)
    private var splashMinTimeElapsed by mutableStateOf(false)
    private var splashVersion by mutableIntStateOf(0)
    private var editorVersion by mutableIntStateOf(0)
    private var permissionSubscription: Subscription? = null
    private var contentPanel: ComposePanel? = null
    private var activeContext: StudioContext? = null

    @JvmStatic
    fun initializeRuntime() {
        MinecraftStageWindow.initializeRuntime()
    }

    @JvmStatic
    fun requestOpen() {
        open()
    }

    @JvmStatic
    fun isUiThreadAvailable(): Boolean = isPlatformReady()

    @JvmStatic
    fun requestToggleMaximize() {
        SwingUtilities.invokeLater { toggleMaximize() }
    }

    @JvmStatic
    fun requestMinimize() {
        SwingUtilities.invokeLater { minimizeWindow() }
    }

    @JvmStatic
    fun requestClose() {
        SwingUtilities.invokeLater { closeWindow() }
    }

    @JvmStatic
    fun notifyWorldClosed() {
        fireWorldClosed()
    }

    @JvmStatic
    fun notifyResourceReload() {
        fireResourceReload()
    }

    @JvmStatic
    fun requestBindDragArea(component: Component?) {
        if (component == null) return
        SwingUtilities.invokeLater { bindDragArea(component) }
    }

    internal fun onDragStart() {
        SwingUtilities.invokeLater { beginFrameDrag() }
    }

    internal fun onDragMove() {
        SwingUtilities.invokeLater { performFrameDrag() }
    }

    internal fun onDragEnd() {
        SwingUtilities.invokeLater { endFrameDrag() }
    }

    internal fun onDragDoubleClick() {
        SwingUtilities.invokeLater { toggleMaximize() }
    }

    override fun onCreated() {
        frame?.title = I18n.get("app:title")

        if (contentPanel == null) {
            contentPanel = ComposePanel().apply {
                setContent { WindowContent() }
            }
        }

        permissionSubscription?.unsubscribe()
        permissionSubscription = AssetEditorClient.sessionState()
            .select({ snapshot -> snapshot.permissions() })
            .subscribe({
                if (state == State.SPLASH)
                    SwingUtilities.invokeLater(::tryTransitionFromSplash)
            }, true)

        enterSplashState()
        contentPanel?.let(::setRoot)
    }

    override fun onWindowFocused() {
        resync()
    }

    override fun onWorldClosed() {
        activeContext?.resetForWorldClose()
        enterSplashState()
        hideWindow()
    }

    override fun onResourceReload() {
        if (state != State.EDITOR) return
        editorVersion++
    }

    private fun enterSplashState() {
        state = State.SPLASH
        splashMinTimeElapsed = false
        splashVersion++
        editorVersion++
    }

    private fun tryTransitionFromSplash() {
        if (!splashMinTimeElapsed || !AssetEditorClient.sessionState().hasReceivedPermissions()) return
        if (state == State.EDITOR) return

        state = State.EDITOR
        editorVersion++
        resync()
    }

    private fun resync() {
        activeContext?.resyncWorldSession()
    }

    @Composable
    private fun WindowContent() {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            when (state) {
                State.SPLASH -> SplashContent(splashVersion)
                State.EDITOR -> EditorContent(editorVersion)
            }
        }
    }

    @Composable
    private fun SplashContent(version: Int) {
        LaunchedEffect(version) {
            delay(1000)
            splashMinTimeElapsed = true
            tryTransitionFromSplash()
        }

        Splash()
    }

    @Composable
    private fun EditorContent(version: Int) {
        val context = remember(version) {
            StudioContext(
                AssetEditorClient.sessionState(),
                AssetEditorClient.sessionDispatch()
            )
        }

        DisposableEffect(context) {
            activeContext = context
            onDispose {
                if (activeContext === context) activeContext = null
                context.dispose()
            }
        }

        LaunchedEffect(context) {
            context.resyncWorldSession()
            context.navigationState().revalidate()
            if (context.navigationState().snapshot().current is NoPermissionDestination) {
                StudioConcept.firstAccessible(AssetEditorClient.sessionState().permissions())?.let { concept ->
                    context.navigationState().navigate(concept.overview())
                }
            }
        }

        CompositionLocalProvider(LocalStudioAssetCache provides context.assetCache()) {
            StudioEditorRoot(context = context)
        }
    }
}

fun Modifier.windowDragArea(id: String): Modifier = this
    .pointerInput(id) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var moved = false
            var clickCount = 0

            VoxelStudioWindow.onDragStart()

            while (true) {
                val event = awaitPointerEvent()

                when (event.type) {
                    PointerEventType.Move -> {
                        if (!moved) moved = true
                        event.changes.forEach { it.consume() }
                        VoxelStudioWindow.onDragMove()
                    }

                    PointerEventType.Release -> {
                        if (moved) {
                            VoxelStudioWindow.onDragEnd()
                        } else {
                            VoxelStudioWindow.onDragEnd()
                            clickCount++
                            if (down.pressed && clickCount == 2) {
                                VoxelStudioWindow.onDragDoubleClick()
                            }
                        }
                        return@awaitEachGesture
                    }
                }
            }
        }
    }

@Composable
fun RememberWindowDragArea(id: String) {
    // Kept for backward compatibility - drag is now handled via pointerInput
}

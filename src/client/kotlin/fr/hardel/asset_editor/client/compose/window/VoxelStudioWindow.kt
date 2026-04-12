package fr.hardel.asset_editor.client.compose.window

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.composed
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import fr.hardel.asset_editor.client.memory.ClientMemoryHolder
import fr.hardel.asset_editor.client.compose.components.layout.editor.StudioEditorRoot
import fr.hardel.asset_editor.client.compose.components.layout.loading.Splash
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.assets.LocalStudioAssetCache
import fr.hardel.asset_editor.client.memory.core.Subscription
import java.awt.Rectangle
import javax.swing.SwingUtilities
import kotlinx.coroutines.delay
import net.minecraft.client.resources.language.I18n
import kotlin.math.roundToInt

object VoxelStudioWindow : MinecraftStageWindow(680, 440) {

    private enum class State { SPLASH, EDITOR }

    private var state by mutableStateOf(State.SPLASH)
    private var splashMinTimeElapsed by mutableStateOf(false)
    private var splashVersion by mutableIntStateOf(0)
    private var editorVersion by mutableIntStateOf(0)
    private var permissionSubscription: Subscription? = null
    private var activeContext: StudioContext? = null

    @JvmStatic
    fun initialize() {
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

    override fun onCreated() {
        composeWindow?.title = I18n.get("app:title")

        permissionSubscription?.unsubscribe()
        permissionSubscription = ClientMemoryHolder.session().subscribe {
            if (state == State.SPLASH)
                SwingUtilities.invokeLater(::tryTransitionFromSplash)
        }
        SwingUtilities.invokeLater(::tryTransitionFromSplash)

        enterSplashState()
        setComposeContent { WindowContent() }
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
        if (!splashMinTimeElapsed || !ClientMemoryHolder.session().hasReceivedPermissions()) return
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
                ClientMemoryHolder.session(),
                ClientMemoryHolder.debug(),
                ClientMemoryHolder.dispatch()
            )
        }

        DisposableEffect(context) {
            activeContext = context
            onDispose {
                if (activeContext === context) activeContext = null
                context.dispose()
            }
        }

        CompositionLocalProvider(LocalStudioAssetCache provides context.assetCache()) {
            StudioEditorRoot(context = context)
        }
    }
}

fun Modifier.windowDragArea(id: String): Modifier = composed {
    DisposableEffect(id) {
        onDispose { VoxelStudioWindow.unregisterDragRegion(id) }
    }

    onGloballyPositioned { coordinates ->
        val origin = coordinates.positionInWindow()
        val size = coordinates.size

        VoxelStudioWindow.registerDragRegion(
            id,
            Rectangle(
                origin.x.roundToInt(),
                origin.y.roundToInt(),
                size.width,
                size.height
            )
        )
    }
}

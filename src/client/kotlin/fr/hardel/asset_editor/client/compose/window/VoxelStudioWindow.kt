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
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.graphics.Color
import com.jetbrains.JBR
import com.jetbrains.WindowDecorations
import fr.hardel.asset_editor.client.memory.ClientMemoryHolder
import fr.hardel.asset_editor.client.compose.components.layout.editor.StudioEditorRoot
import fr.hardel.asset_editor.client.compose.components.layout.loading.Splash
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.assets.LocalStudioAssetCache
import fr.hardel.asset_editor.client.memory.core.Subscription
import java.awt.Frame
import javax.swing.SwingUtilities
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
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
    private var chromeState by mutableStateOf(WindowChromeState.nativeSystem())
    private val titleBarClientAreas = TitleBarClientAreaRegistry()
    private val titleBarHitTestBridge = TitleBarHitTestBridge(
        chromeState = { chromeState },
        customTitleBar = { customTitleBar },
        registry = titleBarClientAreas
    )
    private var customTitleBar: WindowDecorations.CustomTitleBar? = null

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
        frame?.title = I18n.get("app:title")

        if (contentPanel == null) {
            contentPanel = ComposePanel().apply {
                setContent { WindowContent() }
            }
            contentPanel?.let(titleBarHitTestBridge::attach)
        }

        permissionSubscription?.unsubscribe()
        permissionSubscription = ClientMemoryHolder.session().subscribe {
            if (state == State.SPLASH)
                SwingUtilities.invokeLater(::tryTransitionFromSplash)
        }
        SwingUtilities.invokeLater(::tryTransitionFromSplash)

        enterSplashState()
        contentPanel?.let(::setRoot)
        synchronizeChrome()
    }

    override fun onWindowFocused() {
        resync()
    }

    override fun onFrameMetricsChanged() {
        synchronizeChrome()
    }

    override fun onWorldClosed() {
        activeContext?.resetForWorldClose()
        enterSplashState()
        synchronizeChrome()
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
        titleBarClientAreas.clear()
    }

    private fun tryTransitionFromSplash() {
        if (!splashMinTimeElapsed || !ClientMemoryHolder.session().hasReceivedPermissions()) return
        if (state == State.EDITOR) return

        state = State.EDITOR
        editorVersion++
        titleBarClientAreas.clear()
        synchronizeChrome()
        resync()
    }

    private fun resync() {
        activeContext?.resyncWorldSession()
    }

    @Composable
    private fun WindowContent() {
        CompositionLocalProvider(
            LocalWindowChromeState provides chromeState,
            LocalTitleBarClientAreaRegistry provides titleBarClientAreas
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                when (state) {
                    State.SPLASH -> SplashContent(splashVersion)
                    State.EDITOR -> EditorContent(editorVersion)
                }
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

    private fun synchronizeChrome() {
        val currentFrame = frame ?: return
        val environment = WindowChromeResolver.currentEnvironment()
        when (WindowChromeResolver.resolve(environment)) {
            WindowChromeMode.JBR_CUSTOM_TITLEBAR -> applyJbrChrome(currentFrame, environment.platform)
            WindowChromeMode.NATIVE_SYSTEM -> applyNativeChrome(currentFrame)
        }
    }

    private fun applyJbrChrome(
        currentFrame: Frame,
        platform: WindowPlatform
    ) {
        val decorations = runCatching { JBR.getWindowDecorations() }.getOrNull() ?: run {
            applyNativeChrome(currentFrame)
            return
        }
        val titleBar = customTitleBar ?: decorations.createCustomTitleBar().also {
            customTitleBar = it
        }
        val titleBarHeightPx = currentFrame.dpToPixels(currentTitleBarHeightDp())
        titleBar.setHeight(titleBarHeightPx.toFloat())
        titleBar.putProperty("controls.visible", true)
        if (platform == WindowPlatform.WINDOWS) {
            titleBar.putProperty("controls.dark", true)
        }
        decorations.setCustomTitleBar(currentFrame, titleBar)

        chromeState = WindowChromeState.customTitleBar(
            titleBarHeightPx = titleBarHeightPx,
            leftInsetPx = titleBar.leftInset.roundToInt(),
            rightInsetPx = titleBar.rightInset.roundToInt()
        )
    }

    private fun applyNativeChrome(currentFrame: Frame) {
        runCatching { JBR.getWindowDecorations() }
            .getOrNull()
            ?.setCustomTitleBar(currentFrame, null)
        customTitleBar = null
        chromeState = WindowChromeState.nativeSystem()
    }

    private fun currentTitleBarHeightDp(): Int {
        return when (state) {
            State.SPLASH -> WindowChromeDefaults.SPLASH_TITLE_BAR_HEIGHT_DP
            State.EDITOR -> WindowChromeDefaults.EDITOR_TITLE_BAR_HEIGHT_DP
        }
    }

    private fun Frame.dpToPixels(dp: Int): Int {
        val scale = graphicsConfiguration?.defaultTransform?.scaleY ?: 1.0
        return (dp * scale).roundToInt().coerceAtLeast(1)
    }
}

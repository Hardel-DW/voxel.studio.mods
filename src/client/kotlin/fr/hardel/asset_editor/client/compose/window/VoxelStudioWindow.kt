package fr.hardel.asset_editor.client.compose.window

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import fr.hardel.asset_editor.DevFlags
import fr.hardel.asset_editor.client.compose.components.layout.StudioEditorRoot
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.assets.LocalStudioAssetCache
import fr.hardel.asset_editor.client.compose.lib.utils.BrowserUtils
import fr.hardel.asset_editor.client.compose.window.chrome.NativeWindowChrome
import fr.hardel.asset_editor.client.memory.ClientMemoryHolder
import fr.hardel.asset_editor.client.memory.core.Subscription
import fr.hardel.asset_editor.client.splash.SwingSplashPanel
import java.awt.CardLayout
import java.awt.Rectangle
import javax.swing.JPanel
import javax.swing.SwingUtilities
import net.minecraft.client.resources.language.I18n

object VoxelStudioWindow : MinecraftStageWindow(680, 440) {

    private const val HELP_URL = "https://github.com"
    private const val GITHUB_URL = "https://github.com"
    private const val FADE_OUT_MILLIS = 500
    private const val CARD_SPLASH = "splash"
    private const val CARD_EDITOR = "editor"

    private enum class Stage { SPLASH, EDITOR }

    private var stage = Stage.SPLASH
    private var composeReady = false
    private var editorVersion by mutableIntStateOf(0)

    private var splashPanel: SwingSplashPanel? = null
    private var composePanel: ComposePanel? = null
    private var contentContainer: JPanel? = null
    private var cardLayout: CardLayout? = null
    private var activeContext: StudioContext? = null
    private var permissionSubscription: Subscription? = null

    internal val windowChrome: NativeWindowChrome get() = chrome

    @JvmStatic
    fun initialize() = MinecraftStageWindow.initializeRuntime()

    @JvmStatic
    fun requestOpen() = open()

    @JvmStatic
    fun isUiThreadAvailable(): Boolean = isPlatformReady()

    @JvmStatic
    fun requestToggleMaximize() = SwingUtilities.invokeLater(::toggleMaximize)

    @JvmStatic
    fun requestMinimize() = SwingUtilities.invokeLater(::minimizeWindow)

    @JvmStatic
    fun requestClose() = SwingUtilities.invokeLater(::closeWindow)

    @JvmStatic
    fun notifyWorldClosed() = fireWorldClosed()

    @JvmStatic
    fun notifyResourceReload() = fireResourceReload()

    internal fun onDragStart() = SwingUtilities.invokeLater(::beginFrameDrag)
    internal fun onDragMove() = SwingUtilities.invokeLater(::performFrameDrag)
    internal fun onDragEnd() = SwingUtilities.invokeLater(::endFrameDrag)
    internal fun onDragDoubleClick() = SwingUtilities.invokeLater(::toggleMaximize)

    override fun onBeforeShow() = enterSplashStage()

    override fun onCreated() {
        frame?.title = I18n.get("app:title")
        ensureComposePanel()
        composeReady = true
        installPermissionWatcher()
        SwingUtilities.invokeLater(::tryTransitionFromSplash)
    }

    override fun onWindowFocused() = resync()

    override fun onWorldClosed() {
        activeContext?.resetForWorldClose()
        enterSplashStage()
        hideWindow()
    }

    override fun onResourceReload() {
        if (stage == Stage.EDITOR) editorVersion++
    }

    private fun enterSplashStage() {
        stage = Stage.SPLASH
        val container = ensureContentContainer()
        val panel = ensureSplashPanel(container)
        panel.cancelFade()
        panel.startAnimation()
        cardLayout?.show(container, CARD_SPLASH)
    }

    private fun tryTransitionFromSplash() {
        if (stage == Stage.EDITOR) return
        if (DevFlags.STAY_ON_SPLASH) return
        if (!composeReady) return
        if (!ClientMemoryHolder.session().hasReceivedPermissions()) return

        val splash = splashPanel ?: return
        val container = contentContainer ?: return
        composePanel ?: return

        stage = Stage.EDITOR
        editorVersion++
        resync()

        splash.fadeOut(FADE_OUT_MILLIS) {
            cardLayout?.show(container, CARD_EDITOR)
            splash.stopAnimation()
        }
    }

    private fun ensureContentContainer(): JPanel {
        contentContainer?.let { return it }
        val layout = CardLayout()
        val container = JPanel(layout).apply {
            background = java.awt.Color.BLACK
            isOpaque = true
        }
        cardLayout = layout
        contentContainer = container
        setRoot(container)
        return container
    }

    private fun ensureSplashPanel(container: JPanel): SwingSplashPanel {
        splashPanel?.let { return it }
        val panel = SwingSplashPanel(SplashActions)
        attachSwingContent(panel) { p -> panel.isCaptionAt(p.x, p.y) }
        splashPanel = panel
        container.add(panel, CARD_SPLASH)
        return panel
    }

    private fun ensureComposePanel() {
        if (composePanel != null) return
        val container = ensureContentContainer()
        val panel = ComposePanel().apply { setContent { WindowContent() } }
        attachComposeContent(panel)
        composePanel = panel
        container.add(panel, CARD_EDITOR)
    }

    private fun installPermissionWatcher() {
        permissionSubscription?.unsubscribe()
        permissionSubscription = ClientMemoryHolder.session().subscribe {
            if (stage == Stage.SPLASH) SwingUtilities.invokeLater(::tryTransitionFromSplash)
        }
    }

    private fun resync() {
        activeContext?.resyncWorldSession()
    }

    private object SplashActions : SwingSplashPanel.Actions {
        override fun minimize() = requestMinimize()
        override fun maximize() = requestToggleMaximize()
        override fun close() = requestClose()
        override fun openHelp() {
            BrowserUtils.openBrowser(HELP_URL)
        }

        override fun openGithub() {
            BrowserUtils.openBrowser(GITHUB_URL)
        }
        override fun dragStart() = onDragStart()
        override fun dragMove() = onDragMove()
        override fun dragEnd() = onDragEnd()
        override fun dragDoubleClick() = onDragDoubleClick()
    }

    @Composable
    private fun WindowContent() {
        val focusManager = LocalFocusManager.current
        val interactionSource = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(interactionSource = interactionSource, indication = null) {
                    focusManager.clearFocus()
                }
        ) {
            EditorContent(editorVersion)
        }
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

/**
 * Declares a draggable "window caption" rectangle.
 *
 * On platforms where the OS handles the drag natively (Windows via FlatLaf), this registers the
 * bounds into a thread-safe hit-test registry consumed by the AWT-Windows thread. On platforms
 * where we move the frame manually (macOS, Linux), it installs a pointer-input gesture that drives
 * the chrome's begin/perform/end drag lifecycle and maps double-click to maximize.
 *
 * Place this on any Compose area that should be grabbable by the user to move the window.
 * Do NOT stack a clickable on the same region: on Windows the OS hit-test will eat the events
 * before Compose sees them.
 */
fun Modifier.windowDragArea(id: String): Modifier = composed {
    val chrome = VoxelStudioWindow.windowChrome
    if (chrome.nativeDragHandled) {
        DisposableEffect(id) {
            onDispose { chrome.captionRegions.unregister(id) }
        }
        Modifier.onGloballyPositioned { coords ->
            val rect = coords.boundsInRoot()
            chrome.captionRegions.register(
                id,
                Rectangle(rect.left.toInt(), rect.top.toInt(), rect.width.toInt(), rect.height.toInt())
            )
        }
    } else {
        Modifier.pointerInput(id) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                var moved = false
                var clickCount = 0

                VoxelStudioWindow.onDragStart()

                while (true) {
                    val event = awaitPointerEvent()
                    when (event.type) {
                        PointerEventType.Move -> {
                            moved = true
                            event.changes.forEach { it.consume() }
                            VoxelStudioWindow.onDragMove()
                        }

                        PointerEventType.Release -> {
                            VoxelStudioWindow.onDragEnd()
                            if (!moved) {
                                clickCount++
                                if (down.pressed && clickCount == 2) VoxelStudioWindow.onDragDoubleClick()
                            }
                            return@awaitEachGesture
                        }
                    }
                }
            }
        }
    }
}

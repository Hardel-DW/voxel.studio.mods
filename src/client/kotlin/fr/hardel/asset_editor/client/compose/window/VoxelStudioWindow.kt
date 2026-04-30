package fr.hardel.asset_editor.client.compose.window

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.awt.RenderSettings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.components.layout.StudioEditorRoot
import fr.hardel.asset_editor.client.compose.components.ui.FpsCounter
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.shortcut.StudioShortcutBus
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent
import fr.hardel.asset_editor.client.compose.lib.assets.LocalStudioAssetCache
import fr.hardel.asset_editor.client.compose.lib.selectAsFlow
import fr.hardel.asset_editor.client.compose.lib.utils.BrowserUtils
import fr.hardel.asset_editor.client.memory.ClientMemoryHolder
import fr.hardel.asset_editor.client.memory.core.Subscription
import fr.hardel.asset_editor.client.splash.SwingSplashPanel
import java.awt.CardLayout
import javax.swing.JPanel
import javax.swing.SwingUtilities
import net.minecraft.client.resources.language.I18n

object VoxelStudioWindow : StageWindow(680, 440, java.awt.Color(0x101011)) {

    private const val HELP_URL = "https://github.com"
    private const val GITHUB_URL = "https://github.com"
    private const val CARD_SPLASH = "splash"
    private const val CARD_EDITOR = "editor"

    private enum class Stage { SPLASH, EDITOR }

    private var stage = Stage.SPLASH
    private var splashUserAdvanced = false
    private var editorVersion by mutableIntStateOf(0)

    private var splashPanel: SwingSplashPanel? = null
    private var composePanel: ComposePanel? = null
    private var contentContainer: JPanel? = null
    private var cardLayout: CardLayout? = null
    private var activeContext: StudioContext? = null
    private var permissionSubscription: Subscription? = null

    @JvmStatic
    fun initialize() = initializeRuntime()

    @JvmStatic
    fun requestOpen() = open()

    @JvmStatic
    fun isUiThreadAvailable(): Boolean = isPlatformReady()

    @JvmStatic
    fun requestClose() = SwingUtilities.invokeLater { frame?.isVisible = false }

    @JvmStatic
    fun notifyWorldClosed() = fireWorldClosed()

    @JvmStatic
    fun notifyResourceReload() = fireResourceReload()

    override fun onBeforeShow() = enterSplashStage()

    override fun onCreated() {
        frame?.title = I18n.get("app:title")
        ensureComposePanel()
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
        splashUserAdvanced = false
        val container = ensureContentContainer()
        val panel = ensureSplashPanel(container)
        panel.startAnimation()
        cardLayout?.show(container, CARD_SPLASH)
    }

    private fun onSplashUserAdvance() {
        splashUserAdvanced = true
        SwingUtilities.invokeLater(::tryTransitionFromSplash)
    }

    private fun tryTransitionFromSplash() {
        if (stage == Stage.EDITOR) return
        if (ClientMemoryHolder.settings().snapshot().stayOnSplash && !splashUserAdvanced) return
        if (composePanel == null) return
        if (!ClientMemoryHolder.session().hasReceivedPermissions()) return

        val splash = splashPanel ?: return
        val container = contentContainer ?: return

        stage = Stage.EDITOR
        editorVersion++
        resync()

        cardLayout?.show(container, CARD_EDITOR)
        splash.stopAnimation()
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
        splashPanel = panel
        container.add(panel, CARD_SPLASH)
        return panel
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun ensureComposePanel() {
        if (composePanel != null) return
        val container = ensureContentContainer()
        val disableVsync = ClientMemoryHolder.settings().snapshot().disableVsync
        val panel = if (disableVsync) {
            ComposePanel(renderSettings = RenderSettings.SkiaSurface(isVsyncEnabled = false))
        } else {
            ComposePanel()
        }.apply { setContent { WindowContent() } }
        composePanel = panel
        container.add(panel, CARD_EDITOR)
        installShortcutDispatcher()
    }

    private var shortcutDispatcherInstalled = false

    private fun installShortcutDispatcher() {
        if (shortcutDispatcherInstalled) return
        shortcutDispatcherInstalled = true
        val target = frame ?: return
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher { event ->
            if (event.id != KeyEvent.KEY_PRESSED) return@addKeyEventDispatcher false
            val source = event.component ?: return@addKeyEventDispatcher false
            if (SwingUtilities.getWindowAncestor(source) !== target) return@addKeyEventDispatcher false
            if (!StudioShortcutBus.dispatch(event)) return@addKeyEventDispatcher false
            event.consume()
            true
        }
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
        override fun openHelp() {
            BrowserUtils.openBrowser(HELP_URL)
        }

        override fun openGithub() {
            BrowserUtils.openBrowser(GITHUB_URL)
        }

        override fun userAdvance() = onSplashUserAdvance()
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
            Spacer(
                Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(StudioColors.Zinc900)
            )
            val showFps by ClientMemoryHolder.settings()
                .selectAsFlow { it.showFpsCounter }
                .collectAsState(initial = ClientMemoryHolder.settings().snapshot().showFpsCounter)
            if (showFps) {
                FpsCounter(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 12.dp, end = 12.dp)
                )
            }
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

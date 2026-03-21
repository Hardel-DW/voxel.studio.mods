package fr.hardel.asset_editor.client.javafx.window;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.client.AssetEditorClient;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.VoxelResourceLoader;
import fr.hardel.asset_editor.client.javafx.components.layout.editor.StudioEditorRoot;
import fr.hardel.asset_editor.client.javafx.components.layout.loading.Splash;
import fr.hardel.asset_editor.client.state.ClientSessionState;
import fr.hardel.asset_editor.client.selector.Subscription;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.text.Font;
import javafx.util.Duration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class VoxelStudioWindow extends MinecraftStageWindow {

    private static final Logger LOGGER = LoggerFactory.getLogger(VoxelStudioWindow.class);
    private static final double MIN_WIDTH = 680;
    private static final double MIN_HEIGHT = 440;
    private static final List<String> STYLESHEETS = List.of(
        VoxelStudioWindow.class.getResource("/assets/asset_editor/css/splash.css").toExternalForm(),
        VoxelStudioWindow.class.getResource("/assets/asset_editor/css/editor.css").toExternalForm());

    private static VoxelStudioWindow instance;

    private StudioEditorRoot editorRoot;
    private Subscription permissionSubscription;

    private enum State {
        SPLASH, EDITOR
    }

    private State state = State.SPLASH;
    private boolean splashMinTimeElapsed;

    private VoxelStudioWindow() {
        super(MIN_WIDTH, MIN_HEIGHT, STYLESHEETS);
    }

    public static void requestOpen() {
        if (instance == null)
            instance = new VoxelStudioWindow();
        instance.open();
    }

    public static boolean isUiThreadAvailable() {
        return instance != null && instance.isPlatformReady();
    }

    public static void requestToggleMaximize() {
        if (instance != null)
            instance.toggleMaximize();
    }

    public static void notifyWorldClosed() {
        if (instance != null)
            instance.fireWorldClosed();
    }

    public static void notifyResourceReload() {
        if (instance != null)
            instance.fireResourceReload();
    }

    public static void requestBindDragArea(Node dragArea) {
        if (instance != null)
            instance.bindDragArea(dragArea);
    }

    @Override
    protected void onCreated() {
        stage.setTitle(I18n.get("app:title"));
        VoxelResourceLoader.update(Minecraft.getInstance().getResourceManager());
        loadFonts();

        if (permissionSubscription != null)
            permissionSubscription.unsubscribe();

        permissionSubscription = AssetEditorClient.sessionState()
            .select(ClientSessionState.Snapshot::permissions)
            .subscribe(permission -> {
                if (state == State.SPLASH)
                    Platform.runLater(this::tryTransitionFromSplash);
            }, true);

        setRoot(buildSplash());
    }

    @Override
    protected void onWindowFocused() {
        resync();
    }

    @Override
    protected void onWorldClosed() {
        if (editorRoot != null) {
            editorRoot.context().resetForWorldClose();
            editorRoot.dispose();
            editorRoot = null;
        }
        state = State.SPLASH;
        splashMinTimeElapsed = false;
        setRoot(buildSplash());
        hide();
    }

    @Override
    protected void onResourceReload() {
        if (state != State.EDITOR)
            return;
        VoxelResourceLoader.update(Minecraft.getInstance().getResourceManager());
        if (editorRoot != null)
            editorRoot.dispose();
        editorRoot = new StudioEditorRoot(stage, AssetEditorClient.sessionState(), AssetEditorClient.sessionDispatch());
        setRoot(editorRoot);
        resync();
    }

    private Splash buildSplash() {
        splashMinTimeElapsed = false;
        Splash splash = new Splash(stage);
        PauseTransition minDelay = new PauseTransition(Duration.seconds(1));
        minDelay.setOnFinished(event -> {
            splashMinTimeElapsed = true;
            tryTransitionFromSplash();
        });
        minDelay.play();
        return splash;
    }

    private void tryTransitionFromSplash() {
        if (!splashMinTimeElapsed || !AssetEditorClient.sessionState().hasReceivedPermissions())
            return;
        if (state == State.EDITOR)
            return;
        state = State.EDITOR;
        editorRoot = new StudioEditorRoot(stage, AssetEditorClient.sessionState(), AssetEditorClient.sessionDispatch());
        setRoot(editorRoot);
        resync();
    }

    private void resync() {
        if (editorRoot != null)
            editorRoot.context().resyncWorldSession();
    }

    private void loadFonts() {
        for (var variant : VoxelFonts.Variant.values()) {
            Identifier id = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "fonts/" + variant.fileName + ".ttf");
            try (var is = VoxelResourceLoader.open(id)) {
                VoxelFonts.register(variant, Font.loadFont(is, 12));
            } catch (Exception exception) {
                LOGGER.warn("Failed to load font {}: {}", variant.fileName, exception.getMessage());
            }
        }
    }
}

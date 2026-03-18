package fr.hardel.asset_editor.client.javafx;

import fr.hardel.asset_editor.client.ClientPackCache;
import fr.hardel.asset_editor.client.ClientPermissionState;
import fr.hardel.asset_editor.client.ClientSessionDispatch;
import fr.hardel.asset_editor.client.javafx.components.layout.editor.StudioEditorRoot;
import fr.hardel.asset_editor.client.javafx.components.layout.loading.Splash;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VoxelStudioWindow {

    private static final Logger LOGGER = LoggerFactory.getLogger(VoxelStudioWindow.class);
    private static final double MIN_WIDTH = 680;
    private static final double MIN_HEIGHT = 440;

    private static VoxelStudioWindow instance;
    private UndecoratedStageWindow window;
    private Scene scene;
    private StudioEditorRoot editorRoot;
    private boolean platformStarted, splashPlayed, splashMinTimeElapsed;

    public static void open() {
        if (instance == null)
            instance = new VoxelStudioWindow();
        instance.show();
    }

    public static void toggleMaximize() {
        if (instance == null || instance.window == null)
            return;
        instance.window.toggleMaximize();
    }

    public static void onResourceReload() {
        if (instance != null && instance.window != null)
            Platform.runLater(instance::rebuildScene);
    }

    public static void onWorldClosed() {
        if (instance != null && instance.platformStarted)
            Platform.runLater(instance::handleWorldClosed);
    }

    public static void bindDragArea(Node dragArea) {
        if (instance == null || instance.window == null || dragArea == null)
            return;
        instance.window.installDragHandlers(dragArea, instance.scene);
    }

    private void show() {
        if (!platformStarted) {
            platformStarted = true;
            Thread.ofVirtual().name("javafx-init").start(() -> {
                Platform.startup(this::createWindow);
                Platform.setImplicitExit(false);
            });
            return;
        }

        Platform.runLater(() -> {
            if (window == null)
                createWindow();
            else {
                resyncOnOpenOrFocus();
                window.stage().show();
                window.stage().toFront();
            }
        });
    }

    private void createWindow() {
        VoxelResourceLoader.update(Minecraft.getInstance().getResourceManager());
        loadFonts();
        registerCacheListeners();

        Rectangle2D sb = Screen.getPrimary().getVisualBounds();
        double w = Math.max(MIN_WIDTH, sb.getWidth() * 0.75);
        double h = Math.max(MIN_HEIGHT, sb.getHeight() * 0.75);

        Stage stage = new Stage(StageStyle.UNDECORATED);
        stage.setTitle(I18n.get("app:title"));
        stage.setWidth(w);
        stage.setHeight(h);
        stage.setX(sb.getMinX() + (sb.getWidth() - w) / 2);
        stage.setY(sb.getMinY() + (sb.getHeight() - h) / 2);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);

        window = new UndecoratedStageWindow(stage);
        scene = new Scene(initialRoot());
        scene.setFill(Color.BLACK);
        scene.getStylesheets().add(
            VoxelStudioWindow.class.getResource("/assets/asset_editor/css/splash.css").toExternalForm());
        scene.getStylesheets().add(
            VoxelStudioWindow.class.getResource("/assets/asset_editor/css/editor.css").toExternalForm());

        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE)
                stage.hide();
        });

        window.installResizeHandlers(scene);
        stage.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused)
                resyncOnOpenOrFocus();
        });
        stage.setScene(scene);
        stage.show();
        resyncOnOpenOrFocus();
    }

    private void registerCacheListeners() {
        ClientPermissionState.setOnChange(() -> {
            var perms = ClientPermissionState.get();
            Platform.runLater(() -> {
                if (editorRoot != null)
                    editorRoot.context().setPermissions(perms);
                else if (!splashPlayed)
                    tryTransitionFromSplash();
            });
        });

        ClientPackCache.setOnChange(() -> {
            var packs = ClientPackCache.get();
            Platform.runLater(() -> {
                if (editorRoot != null)
                    editorRoot.context().packState().setPacksFromServer(packs);
            });
        });
    }

    private void rebuildScene() {
        if (window == null || scene == null || !splashPlayed)
            return;
        VoxelResourceLoader.update(Minecraft.getInstance().getResourceManager());
        editorRoot = new StudioEditorRoot(window.stage());
        editorRoot.context().setPermissions(ClientPermissionState.get());
        editorRoot.context().packState().setPacksFromServer(ClientPackCache.get());
        scene.setRoot(editorRoot);
        resyncOnOpenOrFocus();
    }

    private Parent initialRoot() {
        if (splashPlayed) {
            editorRoot = new StudioEditorRoot(window.stage());
            return editorRoot;
        }

        splashMinTimeElapsed = false;
        Splash splash = new Splash(window.stage());
        PauseTransition minDelay = new PauseTransition(Duration.seconds(1));
        minDelay.setOnFinished(e -> {
            splashMinTimeElapsed = true;
            tryTransitionFromSplash();
        });
        minDelay.play();
        return splash;
    }

    private void tryTransitionFromSplash() {
        if (!splashMinTimeElapsed || !ClientPermissionState.hasReceived())
            return;
        finishSplash();
    }

    private void finishSplash() {
        if (splashPlayed)
            return;
        splashPlayed = true;
        editorRoot = new StudioEditorRoot(window.stage());
        editorRoot.context().setPermissions(ClientPermissionState.get());
        editorRoot.context().packState().setPacksFromServer(ClientPackCache.get());
        if (scene != null)
            scene.setRoot(editorRoot);
        resyncOnOpenOrFocus();
    }

    private void loadFonts() {
        for (var variant : VoxelFonts.Variant.values()) {
            Identifier id = Identifier.fromNamespaceAndPath("asset_editor", "fonts/" + variant.fileName + ".ttf");
            try (var is = VoxelResourceLoader.open(id)) {
                VoxelFonts.register(variant, Font.loadFont(is, 12));
            } catch (Exception e) {
                LOGGER.warn("Failed to load font {}: {}", variant.fileName, e.getMessage());
            }
        }
    }

    private void resyncOnOpenOrFocus() {
        if (editorRoot == null)
            return;
        editorRoot.context().resyncWorldSession(true);
    }

    private void handleWorldClosed() {
        ClientSessionDispatch.clearGateway();
        if (editorRoot != null)
            editorRoot.context().resetForWorldClose();
        if (window != null)
            window.stage().hide();
    }
}

package fr.hardel.asset_editor.client.javafx;

import fr.hardel.asset_editor.client.AssetEditorClient;
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

    public static boolean isUiThreadAvailable() {
        return instance != null && instance.window != null && instance.scene != null;
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
        AssetEditorClient.sessionState().permissionsProperty().addListener((obs, oldValue, newValue) -> {
            if (!splashPlayed)
                Platform.runLater(this::tryTransitionFromSplash);
        });

        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double width = Math.max(MIN_WIDTH, bounds.getWidth() * 0.75);
        double height = Math.max(MIN_HEIGHT, bounds.getHeight() * 0.75);

        Stage stage = new Stage(StageStyle.UNDECORATED);
        stage.setTitle(I18n.get("app:title"));
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setX(bounds.getMinX() + (bounds.getWidth() - width) / 2);
        stage.setY(bounds.getMinY() + (bounds.getHeight() - height) / 2);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);

        window = new UndecoratedStageWindow(stage);
        scene = new Scene(initialRoot());
        scene.setFill(Color.BLACK);
        scene.getStylesheets().add(
            VoxelStudioWindow.class.getResource("/assets/asset_editor/css/splash.css").toExternalForm());
        scene.getStylesheets().add(
            VoxelStudioWindow.class.getResource("/assets/asset_editor/css/editor.css").toExternalForm());

        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE)
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

    private void rebuildScene() {
        if (window == null || scene == null || !splashPlayed)
            return;
        VoxelResourceLoader.update(Minecraft.getInstance().getResourceManager());
        if (editorRoot != null)
            editorRoot.dispose();
        editorRoot = new StudioEditorRoot(window.stage(), AssetEditorClient.sessionState(), AssetEditorClient.sessionDispatch());
        scene.setRoot(editorRoot);
        resyncOnOpenOrFocus();
    }

    private Parent initialRoot() {
        if (splashPlayed) {
            editorRoot = new StudioEditorRoot(window.stage(), AssetEditorClient.sessionState(), AssetEditorClient.sessionDispatch());
            return editorRoot;
        }

        splashMinTimeElapsed = false;
        Splash splash = new Splash(window.stage());
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
        finishSplash();
    }

    private void finishSplash() {
        if (splashPlayed)
            return;
        splashPlayed = true;
        editorRoot = new StudioEditorRoot(window.stage(), AssetEditorClient.sessionState(), AssetEditorClient.sessionDispatch());
        if (scene != null)
            scene.setRoot(editorRoot);
        resyncOnOpenOrFocus();
    }

    private void loadFonts() {
        for (var variant : VoxelFonts.Variant.values()) {
            Identifier id = Identifier.fromNamespaceAndPath("asset_editor", "fonts/" + variant.fileName + ".ttf");
            try (var is = VoxelResourceLoader.open(id)) {
                VoxelFonts.register(variant, Font.loadFont(is, 12));
            } catch (Exception exception) {
                LOGGER.warn("Failed to load font {}: {}", variant.fileName, exception.getMessage());
            }
        }
    }

    private void resyncOnOpenOrFocus() {
        if (editorRoot == null)
            return;
        editorRoot.context().resyncWorldSession(true);
    }

    private void handleWorldClosed() {
        if (editorRoot != null) {
            editorRoot.context().resetForWorldClose();
            editorRoot.dispose();
            editorRoot = null;
        }
        splashPlayed = false;
        if (scene != null && window != null)
            scene.setRoot(initialRoot());
        if (window != null)
            window.stage().hide();
    }
}

package fr.hardel.asset_editor.client.javafx;

import fr.hardel.asset_editor.client.javafx.components.layout.editor.StudioEditorRoot;
import fr.hardel.asset_editor.client.javafx.components.layout.loading.Splash;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

/**
 * Manages the JavaFX Stage lifecycle.
 * Uses UNDECORATED style so TitleBar provides custom window chrome.
 * Implements edge-based resize for all 8 directions.
 */
public final class VoxelStudioWindow {

    private static final int RESIZE_MARGIN = 10;
    private static final int DRAG_TOP_HEIGHT = 48;
    private static final int DRAG_LEFT_WIDTH = 64;
    private static final int SNAP_MARGIN = 28;
    private static final double MIN_WINDOW_WIDTH = 680;
    private static final double MIN_WINDOW_HEIGHT = 440;

    private static VoxelStudioWindow instance;
    private Stage stage;
    private Scene scene;
    private StudioEditorRoot editorRoot;
    private boolean platformStarted = false;
    private boolean splashPlayed = false;

    private double resizeDragX, resizeDragY;
    private double resizeDragW, resizeDragH;
    private double resizeDragStageX, resizeDragStageY;
    private double moveDragOffsetX, moveDragOffsetY;
    private boolean resizingWindow = false;
    private boolean draggingWindow = false;
    private boolean suppressNextClick = false;
    private ResizeZone activeZone = ResizeZone.NONE;

    public static void open() {
        if (instance == null)
            instance = new VoxelStudioWindow();
        instance.show();
    }

    public static void onResourceReload() {
        if (instance != null && instance.stage != null) {
            Platform.runLater(instance::rebuildScene);
        }
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
            if (stage == null)
                createWindow();
            else {
                stage.show();
                stage.toFront();
            }
        });
    }

    private void createWindow() {
        ResourceLoader.update(Minecraft.getInstance().getResourceManager());
        loadRubikFont();
        loadMinecraftFonts();

        stage = new Stage(StageStyle.UNDECORATED);
        stage.setTitle(I18n.get("tauri:app.title"));
        stage.setWidth(900);
        stage.setHeight(600);
        stage.setMinWidth(MIN_WINDOW_WIDTH);
        stage.setMinHeight(MIN_WINDOW_HEIGHT);

        scene = new Scene(initialRoot());
        scene.setFill(Color.BLACK);
        scene.getStylesheets().add(
                VoxelStudioWindow.class.getResource("/assets/asset_editor/css/splash.css").toExternalForm());
        scene.getStylesheets().add(
                VoxelStudioWindow.class.getResource("/assets/asset_editor/css/editor.css").toExternalForm());

        attachResizeHandlers(scene);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE)
                stage.hide();
        });
        stage.setScene(scene);
        stage.show();
    }

    private void rebuildScene() {
        if (stage == null || scene == null)
            return;
        if (!splashPlayed)
            return;
        editorRoot = new StudioEditorRoot(stage);
        scene.setRoot(editorRoot);
    }

    private Parent initialRoot() {
        if (splashPlayed) {
            editorRoot = new StudioEditorRoot(stage);
            return editorRoot;
        }
        Splash splash = new Splash(stage);
        PauseTransition delay = new PauseTransition(Duration.seconds(3));
        delay.setOnFinished(e -> {
            splashPlayed = true;
            editorRoot = new StudioEditorRoot(stage);
            if (scene != null)
                scene.setRoot(editorRoot);
        });
        delay.play();
        return splash;
    }

    private void loadRubikFont() {
        for (VoxelFonts.Rubik weight : VoxelFonts.Rubik.values()) {
            Identifier id = Identifier.fromNamespaceAndPath("asset_editor", "fonts/" + weight.fileName + ".ttf");
            try (var is = ResourceLoader.open(id)) {
                VoxelFonts.register(weight, Font.loadFont(is, 12));
            } catch (Exception ignored) {}
        }
    }

    private void loadMinecraftFonts() {
        for (VoxelFonts.Minecraft font : VoxelFonts.Minecraft.values()) {
            Identifier id = Identifier.fromNamespaceAndPath("asset_editor", "fonts/" + font.fileName + ".ttf");
            try (var is = ResourceLoader.open(id)) {
                VoxelFonts.registerMinecraft(font, Font.loadFont(is, 12));
            } catch (Exception ignored) {}
        }
    }

    private void attachResizeHandlers(Scene scene) {
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, e -> {
            if (draggingWindow)
                return;
            if (stage.isMaximized()) {
                scene.setCursor(Cursor.DEFAULT);
                return;
            }
            ResizeZone zone = detectZone(e.getX(), e.getY(), scene.getWidth(), scene.getHeight());
            scene.setCursor(zone.cursor());
            if (zone != ResizeZone.NONE)
                e.consume();
        });

        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            suppressNextClick = false;
            activeZone = detectZone(e.getX(), e.getY(), scene.getWidth(), scene.getHeight());
            draggingWindow = false;
            resizingWindow = false;

            if (activeZone != ResizeZone.NONE && !stage.isMaximized()) {
                resizingWindow = true;
                resizeDragX = e.getScreenX();
                resizeDragY = e.getScreenY();
                resizeDragW = stage.getWidth();
                resizeDragH = stage.getHeight();
                resizeDragStageX = stage.getX();
                resizeDragStageY = stage.getY();
                e.consume();
                return;
            }
            activeZone = ResizeZone.NONE;

            if (isDragZone(e.getX(), e.getY()) && !isInteractiveTarget(e.getTarget())) {
                draggingWindow = true;
                moveDragOffsetX = e.getScreenX() - stage.getX();
                moveDragOffsetY = e.getScreenY() - stage.getY();
            }
        });

        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if (resizingWindow && activeZone != ResizeZone.NONE && !stage.isMaximized()) {
                double dx = e.getScreenX() - resizeDragX;
                double dy = e.getScreenY() - resizeDragY;
                activeZone.apply(stage, dx, dy, resizeDragW, resizeDragH, resizeDragStageX, resizeDragStageY,
                        stage.getMinWidth(), stage.getMinHeight());
                e.consume();
                return;
            }
            if (!draggingWindow)
                return;

            if (stage.isMaximized()) {
                double ratio = Math.max(0.1, Math.min(0.9, e.getX() / Math.max(1, scene.getWidth())));
                stage.setMaximized(false);
                moveDragOffsetX = stage.getWidth() * ratio;
                moveDragOffsetY = Math.min(24, e.getY());
            }

            stage.setX(e.getScreenX() - moveDragOffsetX);
            stage.setY(e.getScreenY() - moveDragOffsetY);
            e.consume();
        });

        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            boolean wasResizing = resizingWindow;
            if (draggingWindow)
                applySnap(e.getScreenX(), e.getScreenY());
            activeZone = ResizeZone.NONE;
            resizingWindow = false;
            draggingWindow = false;
            scene.setCursor(Cursor.DEFAULT);
            if (wasResizing) {
                suppressNextClick = true;
                e.consume();
            }
        });

        scene.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            if (suppressNextClick) {
                suppressNextClick = false;
                e.consume();
                return;
            }
            if (e.getClickCount() != 2)
                return;
            if (!isDragZone(e.getX(), e.getY()) || isInteractiveTarget(e.getTarget()))
                return;
            stage.setMaximized(!stage.isMaximized());
            e.consume();
        });
    }

    private ResizeZone detectZone(double x, double y, double w, double h) {
        boolean n = y <= RESIZE_MARGIN;
        boolean s = y >= h - RESIZE_MARGIN;
        boolean west = x <= RESIZE_MARGIN;
        boolean east = x >= w - RESIZE_MARGIN;
        if (n && west)
            return ResizeZone.NW;
        if (n && east)
            return ResizeZone.NE;
        if (s && west)
            return ResizeZone.SW;
        if (s && east)
            return ResizeZone.SE;
        if (n)
            return ResizeZone.N;
        if (s)
            return ResizeZone.S;
        if (west)
            return ResizeZone.W;
        if (east)
            return ResizeZone.E;
        return ResizeZone.NONE;
    }

    private boolean isDragZone(double x, double y) {
        return y <= DRAG_TOP_HEIGHT || x <= DRAG_LEFT_WIDTH;
    }

    private static boolean isInteractiveTarget(Object target) {
        if (!(target instanceof Node node))
            return false;
        Node current = node;
        while (current != null) {
            if (current.getOnMouseClicked() != null || current.getOnMousePressed() != null
                    || current.getOnMouseReleased() != null) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private void applySnap(double screenX, double screenY) {
        if (stage.isMaximized())
            return;

        Screen screen = Screen.getScreensForRectangle(screenX, screenY, 1, 1).stream()
                .findFirst()
                .orElse(Screen.getPrimary());
        Rectangle2D bounds = screen.getVisualBounds();

        boolean left = screenX <= bounds.getMinX() + SNAP_MARGIN;
        boolean right = screenX >= bounds.getMaxX() - SNAP_MARGIN;
        boolean top = screenY <= bounds.getMinY() + SNAP_MARGIN;
        boolean bottom = screenY >= bounds.getMaxY() - SNAP_MARGIN;

        if (top && !left && !right) {
            stage.setMaximized(true);
            return;
        }

        stage.setMaximized(false);
        if (left && top) {
            snap(bounds, bounds.getMinX(), bounds.getMinY(), bounds.getWidth() / 2, bounds.getHeight() / 2);
            return;
        }
        if (right && top) {
            snap(bounds, bounds.getMinX() + bounds.getWidth() / 2, bounds.getMinY(),
                    bounds.getWidth() / 2, bounds.getHeight() / 2);
            return;
        }
        if (left && bottom) {
            snap(bounds, bounds.getMinX(), bounds.getMinY() + bounds.getHeight() / 2,
                    bounds.getWidth() / 2, bounds.getHeight() / 2);
            return;
        }
        if (right && bottom) {
            snap(bounds, bounds.getMinX() + bounds.getWidth() / 2, bounds.getMinY() + bounds.getHeight() / 2,
                    bounds.getWidth() / 2, bounds.getHeight() / 2);
            return;
        }
        if (left) {
            snap(bounds, bounds.getMinX(), bounds.getMinY(), bounds.getWidth() / 2, bounds.getHeight());
            return;
        }
        if (right) {
            snap(bounds, bounds.getMinX() + bounds.getWidth() / 2, bounds.getMinY(),
                    bounds.getWidth() / 2, bounds.getHeight());
        }
    }

    private void snap(Rectangle2D bounds, double x, double y, double w, double h) {
        double width = Math.min(bounds.getWidth(), Math.max(stage.getMinWidth(), w));
        double height = Math.min(bounds.getHeight(), Math.max(stage.getMinHeight(), h));
        double clampedX = Math.max(bounds.getMinX(), Math.min(x, bounds.getMaxX() - width));
        double clampedY = Math.max(bounds.getMinY(), Math.min(y, bounds.getMaxY() - height));
        stage.setX(clampedX);
        stage.setY(clampedY);
        stage.setWidth(width);
        stage.setHeight(height);
    }

    private enum ResizeZone {
        NONE {
            @Override
            public Cursor cursor() {
                return Cursor.DEFAULT;
            }

            @Override
            public void apply(Stage s, double dx, double dy, double ow, double oh, double ox, double oy, double mw,
                    double mh) {
            }
        },
        N {
            @Override
            public Cursor cursor() {
                return Cursor.N_RESIZE;
            }

            @Override
            public void apply(Stage s, double dx, double dy, double ow, double oh, double ox, double oy, double mw,
                    double mh) {
                double newH = Math.max(mh, oh - dy);
                s.setY(oy + (oh - newH));
                s.setHeight(newH);
            }
        },
        S {
            @Override
            public Cursor cursor() {
                return Cursor.S_RESIZE;
            }

            @Override
            public void apply(Stage s, double dx, double dy, double ow, double oh, double ox, double oy, double mw,
                    double mh) {
                s.setHeight(Math.max(mh, oh + dy));
            }
        },
        W {
            @Override
            public Cursor cursor() {
                return Cursor.W_RESIZE;
            }

            @Override
            public void apply(Stage s, double dx, double dy, double ow, double oh, double ox, double oy, double mw,
                    double mh) {
                double newW = Math.max(mw, ow - dx);
                s.setX(ox + (ow - newW));
                s.setWidth(newW);
            }
        },
        E {
            @Override
            public Cursor cursor() {
                return Cursor.E_RESIZE;
            }

            @Override
            public void apply(Stage s, double dx, double dy, double ow, double oh, double ox, double oy, double mw,
                    double mh) {
                s.setWidth(Math.max(mw, ow + dx));
            }
        },
        NW {
            @Override
            public Cursor cursor() {
                return Cursor.NW_RESIZE;
            }

            @Override
            public void apply(Stage s, double dx, double dy, double ow, double oh, double ox, double oy, double mw,
                    double mh) {
                N.apply(s, dx, dy, ow, oh, ox, oy, mw, mh);
                W.apply(s, dx, dy, ow, oh, ox, oy, mw, mh);
            }
        },
        NE {
            @Override
            public Cursor cursor() {
                return Cursor.NE_RESIZE;
            }

            @Override
            public void apply(Stage s, double dx, double dy, double ow, double oh, double ox, double oy, double mw,
                    double mh) {
                N.apply(s, dx, dy, ow, oh, ox, oy, mw, mh);
                E.apply(s, dx, dy, ow, oh, ox, oy, mw, mh);
            }
        },
        SW {
            @Override
            public Cursor cursor() {
                return Cursor.SW_RESIZE;
            }

            @Override
            public void apply(Stage s, double dx, double dy, double ow, double oh, double ox, double oy, double mw,
                    double mh) {
                S.apply(s, dx, dy, ow, oh, ox, oy, mw, mh);
                W.apply(s, dx, dy, ow, oh, ox, oy, mw, mh);
            }
        },
        SE {
            @Override
            public Cursor cursor() {
                return Cursor.SE_RESIZE;
            }

            @Override
            public void apply(Stage s, double dx, double dy, double ow, double oh, double ox, double oy, double mw,
                    double mh) {
                S.apply(s, dx, dy, ow, oh, ox, oy, mw, mh);
                E.apply(s, dx, dy, ow, oh, ox, oy, mw, mh);
            }
        };

        public abstract Cursor cursor();

        public abstract void apply(Stage stage, double dx, double dy, double origW, double origH,
                double origX, double origY, double minW, double minH);
    }
}



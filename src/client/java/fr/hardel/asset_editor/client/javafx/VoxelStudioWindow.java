package fr.hardel.asset_editor.client.javafx;

import fr.hardel.asset_editor.client.javafx.components.layout.editor.StudioEditorRoot;
import fr.hardel.asset_editor.client.javafx.components.layout.loading.Splash;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
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

public final class VoxelStudioWindow {

    private static final int RESIZE_MARGIN = 10;
    private static final int DRAG_TOP_HEIGHT = 48;
    private static final int DRAG_LEFT_WIDTH = 64;
    private static final int SNAP_MARGIN = 5;
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
    private Rectangle2D boundsBeforeSnap;
    private Node cursorOverrideNode;
    private Cursor cursorOverrideOriginal;

    public static void open() {
        if (instance == null)
            instance = new VoxelStudioWindow();
        instance.show();
    }

    public static void toggleMaximize() {
        if (instance == null || instance.stage == null) return;
        if (instance.isSnapped()) {
            instance.unsnap();
        } else {
            instance.snapTo(instance.windowScreen().getVisualBounds());
        }
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

    private boolean isSnapped() { return boundsBeforeSnap != null; }

    private void createWindow() {
        ResourceLoader.update(Minecraft.getInstance().getResourceManager());
        loadRubikFont();
        loadMinecraftFonts();

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double initW = Math.max(MIN_WINDOW_WIDTH, screenBounds.getWidth() * 0.75);
        double initH = Math.max(MIN_WINDOW_HEIGHT, screenBounds.getHeight() * 0.75);

        stage = new Stage(StageStyle.UNDECORATED);
        stage.setTitle(I18n.get("tauri:app.title"));
        stage.setWidth(initW);
        stage.setHeight(initH);
        stage.setX(screenBounds.getMinX() + (screenBounds.getWidth() - initW) / 2);
        stage.setY(screenBounds.getMinY() + (screenBounds.getHeight() - initH) / 2);
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
        if (stage == null || scene == null || !splashPlayed) return;
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
            if (scene != null) scene.setRoot(editorRoot);
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

    private void unsnap() {
        if (boundsBeforeSnap == null) return;
        stage.setX(boundsBeforeSnap.getMinX());
        stage.setY(boundsBeforeSnap.getMinY());
        stage.setWidth(boundsBeforeSnap.getWidth());
        stage.setHeight(boundsBeforeSnap.getHeight());
        boundsBeforeSnap = null;
    }

    private void snapTo(Rectangle2D region) {
        if (boundsBeforeSnap == null)
            boundsBeforeSnap = new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
        stage.setX(region.getMinX());
        stage.setY(region.getMinY());
        stage.setWidth(region.getWidth());
        stage.setHeight(region.getHeight());
    }

    private void attachResizeHandlers(Scene scene) {
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, e -> {
            if (draggingWindow || resizingWindow) return;
            if (isSnapped()) {
                clearCursorOverride();
                scene.setCursor(Cursor.DEFAULT);
                return;
            }
            ResizeZone zone = detectZone(e.getX(), e.getY(), scene.getWidth(), scene.getHeight());
            scene.setCursor(zone.cursor());
            if (zone != ResizeZone.NONE)
                applyCursorOverride(e.getTarget(), zone.cursor());
            else
                clearCursorOverride();
        });

        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            suppressNextClick = false;
            ResizeZone zone = detectZone(e.getX(), e.getY(), scene.getWidth(), scene.getHeight());
            draggingWindow = false;
            resizingWindow = false;

            if (zone != ResizeZone.NONE && !isSnapped()) {
                activeZone = zone;
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
            if (resizingWindow && activeZone != ResizeZone.NONE) {
                double dx = e.getScreenX() - resizeDragX;
                double dy = e.getScreenY() - resizeDragY;
                activeZone.apply(stage, dx, dy, resizeDragW, resizeDragH, resizeDragStageX, resizeDragStageY,
                        stage.getMinWidth(), stage.getMinHeight());
                e.consume();
                return;
            }
            if (!draggingWindow) return;

            if (isSnapped()) {
                double prevW = scene.getWidth();
                unsnap();
                double ratio = Math.max(0.1, Math.min(0.9, e.getX() / Math.max(1, prevW)));
                moveDragOffsetX = stage.getWidth() * ratio;
                moveDragOffsetY = Math.min(24, e.getY());
            }

            stage.setX(e.getScreenX() - moveDragOffsetX);
            stage.setY(e.getScreenY() - moveDragOffsetY);
            e.consume();
        });

        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            boolean wasResizing = resizingWindow;
            if (draggingWindow) applySnap(e.getScreenX(), e.getScreenY());
            activeZone = ResizeZone.NONE;
            resizingWindow = false;
            draggingWindow = false;
            clearCursorOverride();
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
            if (e.getClickCount() != 2) return;
            if (!isDragZone(e.getX(), e.getY()) || isInteractiveTarget(e.getTarget())) return;
            if (isSnapped()) {
                unsnap();
            } else {
                Rectangle2D sb = windowScreen().getVisualBounds();
                snapTo(sb);
            }
            e.consume();
        });
    }

    private ResizeZone detectZone(double x, double y, double w, double h) {
        boolean n = y <= RESIZE_MARGIN;
        boolean s = y >= h - RESIZE_MARGIN;
        boolean west = x <= RESIZE_MARGIN;
        boolean east = x >= w - RESIZE_MARGIN;
        if (n && west) return ResizeZone.NW;
        if (n && east) return ResizeZone.NE;
        if (s && west) return ResizeZone.SW;
        if (s && east) return ResizeZone.SE;
        if (n) return ResizeZone.N;
        if (s) return ResizeZone.S;
        if (west) return ResizeZone.W;
        if (east) return ResizeZone.E;
        return ResizeZone.NONE;
    }

    private boolean isDragZone(double x, double y) {
        return y <= DRAG_TOP_HEIGHT || x <= DRAG_LEFT_WIDTH;
    }

    private static boolean isInteractiveTarget(Object target) {
        if (!(target instanceof Node node)) return false;
        Node current = node;
        while (current != null) {
            if (current.getCursor() == Cursor.HAND
                    || current.getOnMouseClicked() != null
                    || current.getOnMousePressed() != null
                    || current.getOnMouseReleased() != null
                    || current instanceof javafx.scene.control.ButtonBase) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private void applyCursorOverride(Object target, Cursor cursor) {
        clearCursorOverride();
        if (!(target instanceof Node node)) return;
        while (node != null && node.getCursor() == null) node = node.getParent();
        if (node == null || node.getCursor() == cursor) return;
        cursorOverrideNode = node;
        cursorOverrideOriginal = node.getCursor();
        node.setCursor(cursor);
    }

    private void clearCursorOverride() {
        if (cursorOverrideNode == null) return;
        cursorOverrideNode.setCursor(cursorOverrideOriginal);
        cursorOverrideNode = null;
        cursorOverrideOriginal = null;
    }

    private Screen windowScreen() {
        double wx = stage.getX(), wy = stage.getY();
        double wr = wx + stage.getWidth(), wb = wy + stage.getHeight();
        Screen best = Screen.getPrimary();
        double bestArea = 0;
        for (Screen s : Screen.getScreens()) {
            Rectangle2D b = s.getVisualBounds();
            double overlap = Math.max(0, Math.min(wr, b.getMaxX()) - Math.max(wx, b.getMinX()))
                           * Math.max(0, Math.min(wb, b.getMaxY()) - Math.max(wy, b.getMinY()));
            if (overlap > bestArea) {
                bestArea = overlap;
                best = s;
            }
        }
        return best;
    }

    private void applySnap(double cursorX, double cursorY) {
        if (isSnapped()) return;

        Rectangle2D bounds = windowScreen().getVisualBounds();

        boolean left = cursorX <= bounds.getMinX() + SNAP_MARGIN;
        boolean right = cursorX >= bounds.getMaxX() - SNAP_MARGIN;
        boolean top = cursorY <= bounds.getMinY() + SNAP_MARGIN;
        boolean bottom = cursorY >= bounds.getMaxY() - SNAP_MARGIN;

        if (!left && !right && !top && !bottom) return;

        double hw = bounds.getWidth() / 2, hh = bounds.getHeight() / 2;

        if (top && !left && !right) {
            snapTo(bounds);
        } else if (left && top) {
            snapTo(new Rectangle2D(bounds.getMinX(), bounds.getMinY(), hw, hh));
        } else if (right && top) {
            snapTo(new Rectangle2D(bounds.getMinX() + hw, bounds.getMinY(), hw, hh));
        } else if (left && bottom) {
            snapTo(new Rectangle2D(bounds.getMinX(), bounds.getMinY() + hh, hw, hh));
        } else if (right && bottom) {
            snapTo(new Rectangle2D(bounds.getMinX() + hw, bounds.getMinY() + hh, hw, hh));
        } else if (left) {
            snapTo(new Rectangle2D(bounds.getMinX(), bounds.getMinY(), hw, bounds.getHeight()));
        } else if (right) {
            snapTo(new Rectangle2D(bounds.getMinX() + hw, bounds.getMinY(), hw, bounds.getHeight()));
        } else if (bottom) {
            snapTo(new Rectangle2D(bounds.getMinX(), bounds.getMinY() + hh, bounds.getWidth(), hh));
        }
    }

    private enum ResizeZone {
        NONE {
            @Override public Cursor cursor() { return Cursor.DEFAULT; }
            @Override public void apply(Stage s, double dx, double dy, double ow, double oh, double ox, double oy, double mw, double mh) {}
        },
        N {
            @Override public Cursor cursor() { return Cursor.N_RESIZE; }
            @Override public void apply(Stage s, double dx, double dy, double ow, double oh, double ox, double oy, double mw, double mh) {
                double newH = Math.max(mh, oh - dy);
                s.setY(oy + (oh - newH));
                s.setHeight(newH);
            }
        },
        S {
            @Override public Cursor cursor() { return Cursor.S_RESIZE; }
            @Override public void apply(Stage s, double dx, double dy, double ow, double oh, double ox, double oy, double mw, double mh) {
                s.setHeight(Math.max(mh, oh + dy));
            }
        },
        W {
            @Override public Cursor cursor() { return Cursor.W_RESIZE; }
            @Override public void apply(Stage s, double dx, double dy, double ow, double oh, double ox, double oy, double mw, double mh) {
                double newW = Math.max(mw, ow - dx);
                s.setX(ox + (ow - newW));
                s.setWidth(newW);
            }
        },
        E {
            @Override public Cursor cursor() { return Cursor.E_RESIZE; }
            @Override public void apply(Stage s, double dx, double dy, double ow, double oh, double ox, double oy, double mw, double mh) {
                s.setWidth(Math.max(mw, ow + dx));
            }
        },
        NW {
            @Override public Cursor cursor() { return Cursor.NW_RESIZE; }
            @Override public void apply(Stage s, double dx, double dy, double ow, double oh, double ox, double oy, double mw, double mh) {
                N.apply(s, dx, dy, ow, oh, ox, oy, mw, mh);
                W.apply(s, dx, dy, ow, oh, ox, oy, mw, mh);
            }
        },
        NE {
            @Override public Cursor cursor() { return Cursor.NE_RESIZE; }
            @Override public void apply(Stage s, double dx, double dy, double ow, double oh, double ox, double oy, double mw, double mh) {
                N.apply(s, dx, dy, ow, oh, ox, oy, mw, mh);
                E.apply(s, dx, dy, ow, oh, ox, oy, mw, mh);
            }
        },
        SW {
            @Override public Cursor cursor() { return Cursor.SW_RESIZE; }
            @Override public void apply(Stage s, double dx, double dy, double ow, double oh, double ox, double oy, double mw, double mh) {
                S.apply(s, dx, dy, ow, oh, ox, oy, mw, mh);
                W.apply(s, dx, dy, ow, oh, ox, oy, mw, mh);
            }
        },
        SE {
            @Override public Cursor cursor() { return Cursor.SE_RESIZE; }
            @Override public void apply(Stage s, double dx, double dy, double ow, double oh, double ox, double oy, double mw, double mh) {
                S.apply(s, dx, dy, ow, oh, ox, oy, mw, mh);
                E.apply(s, dx, dy, ow, oh, ox, oy, mw, mh);
            }
        };

        public abstract Cursor cursor();
        public abstract void apply(Stage stage, double dx, double dy, double origW, double origH,
                double origX, double origY, double minW, double minH);
    }
}

package fr.hardel.asset_editor.client.javafx;

import fr.hardel.asset_editor.client.javafx.layout.Splash;
import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

/**
 * Manages the JavaFX Stage lifecycle.
 * Uses UNDECORATED style so TitleBar provides custom window chrome.
 * Implements edge-based resize for all 8 directions.
 */
public final class VoxelStudioWindow {

    private static final int RESIZE_MARGIN = 6;

    private static VoxelStudioWindow instance;
    private Stage stage;
    private boolean platformStarted = false;

    private double resizeDragX, resizeDragY;
    private double resizeDragW, resizeDragH;
    private double resizeDragStageX, resizeDragStageY;
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

        stage = new Stage(StageStyle.UNDECORATED);
        stage.setTitle(I18n.get("tauri:app.title"));
        stage.setWidth(900);
        stage.setHeight(600);
        stage.setMinWidth(640);
        stage.setMinHeight(400);

        Scene scene = new Scene(new Splash(stage));
        scene.setFill(Color.BLACK);
        scene.getStylesheets().add(
                VoxelStudioWindow.class.getResource("/assets/asset_editor/css/splash.css").toExternalForm());

        attachResizeHandlers(scene);
        stage.setScene(scene);
        stage.show();
    }

    private void rebuildScene() {
        if (stage == null)
            return;
        stage.getScene().setRoot(new Splash(stage));
    }

    private void loadRubikFont() {
        for (VoxelFonts.Rubik weight : VoxelFonts.Rubik.values()) {
            Identifier id = Identifier.fromNamespaceAndPath("asset_editor", "fonts/" + weight.fileName + ".ttf");
            try (var is = ResourceLoader.open(id)) {
                VoxelFonts.register(weight, Font.loadFont(is, 12));
            } catch (Exception ignored) {}
        }
    }

    private void attachResizeHandlers(Scene scene) {
        scene.setOnMouseMoved(e -> {
            ResizeZone zone = detectZone(e.getX(), e.getY(), stage.getWidth(), stage.getHeight());
            scene.setCursor(zone.cursor());
        });

        scene.setOnMousePressed(e -> {
            activeZone = detectZone(e.getX(), e.getY(), stage.getWidth(), stage.getHeight());
            if (activeZone != ResizeZone.NONE) {
                resizeDragX = e.getScreenX();
                resizeDragY = e.getScreenY();
                resizeDragW = stage.getWidth();
                resizeDragH = stage.getHeight();
                resizeDragStageX = stage.getX();
                resizeDragStageY = stage.getY();
            }
        });

        scene.setOnMouseDragged(e -> {
            if (activeZone == ResizeZone.NONE || stage.isMaximized())
                return;
            double dx = e.getScreenX() - resizeDragX;
            double dy = e.getScreenY() - resizeDragY;
            activeZone.apply(stage, dx, dy, resizeDragW, resizeDragH, resizeDragStageX, resizeDragStageY,
                    stage.getMinWidth(), stage.getMinHeight());
        });

        scene.setOnMouseReleased(e -> activeZone = ResizeZone.NONE);
    }

    private ResizeZone detectZone(double x, double y, double w, double h) {
        boolean n = y < RESIZE_MARGIN;
        boolean s = y > h - RESIZE_MARGIN;
        boolean west = x < RESIZE_MARGIN;
        boolean east = x > w - RESIZE_MARGIN;
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

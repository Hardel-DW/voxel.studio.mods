package fr.hardel.asset_editor.client.javafx.window;

import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.List;

public class UndecoratedStageWindow {

    private static final int RESIZE_MARGIN = 10;
    private static final int SNAP_MARGIN = 5;

    protected final Stage stage;
    protected Scene scene;

    private ResizeZone activeZone = ResizeZone.NONE;
    private boolean resizing, dragging;
    private boolean suppressNextClick;
    private double dragX, dragY, dragW, dragH, dragStageX, dragStageY;
    private double moveOffsetX, moveOffsetY;

    private Rectangle2D boundsBeforeSnap;
    private Node cursorOverrideNode;
    private Cursor cursorOverrideOriginal;

    protected UndecoratedStageWindow(double minWidth, double minHeight, List<String> stylesheets) {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double width = Math.max(minWidth, bounds.getWidth() * 0.75);
        double height = Math.max(minHeight, bounds.getHeight() * 0.75);

        stage = new Stage(StageStyle.UNDECORATED);
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setX(bounds.getMinX() + (bounds.getWidth() - width) / 2);
        stage.setY(bounds.getMinY() + (bounds.getHeight() - height) / 2);
        stage.setMinWidth(minWidth);
        stage.setMinHeight(minHeight);

        scene = new Scene(new javafx.scene.layout.StackPane());
        scene.setFill(Color.BLACK);
        for (String css : stylesheets) {
            scene.getStylesheets().add(css);
        }

        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE)
                stage.hide();
        });

        installResizeHandlers();
        stage.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused) onWindowFocused();
        });
        stage.setScene(scene);
    }

    public Stage stage() {
        return stage;
    }

    public Scene scene() {
        return scene;
    }

    public boolean isSnapped() {
        return boundsBeforeSnap != null;
    }

    public void setRoot(Parent root) {
        scene.setRoot(root);
    }

    public void show() {
        stage.show();
        stage.toFront();
    }

    public void hide() {
        stage.hide();
    }

    public void toggleMaximize() {
        if (isSnapped())
            unsnap();
        else
            snapTo(windowScreen().getVisualBounds());
    }

    public void bindDragArea(Node dragArea) {
        if (dragArea == null) return;
        installDragHandlers(dragArea);
    }

    protected void onWindowFocused() {}

    public void snapTo(Rectangle2D region) {
        if (boundsBeforeSnap == null)
            boundsBeforeSnap = new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());

        stage.setX(region.getMinX());
        stage.setY(region.getMinY());
        stage.setWidth(region.getWidth());
        stage.setHeight(region.getHeight());
    }

    public void unsnap() {
        if (boundsBeforeSnap == null)
            return;

        stage.setX(boundsBeforeSnap.getMinX());
        stage.setY(boundsBeforeSnap.getMinY());
        stage.setWidth(boundsBeforeSnap.getWidth());
        stage.setHeight(boundsBeforeSnap.getHeight());
        boundsBeforeSnap = null;
    }

    private void installResizeHandlers() {
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, e -> {
            if (dragging || resizing)
                return;
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
            dragging = false;
            resizing = false;

            ResizeZone zone = detectZone(e.getX(), e.getY(), scene.getWidth(), scene.getHeight());
            if (zone != ResizeZone.NONE && !isSnapped()) {
                activeZone = zone;
                resizing = true;
                dragX = e.getScreenX();
                dragY = e.getScreenY();
                dragW = stage.getWidth();
                dragH = stage.getHeight();
                dragStageX = stage.getX();
                dragStageY = stage.getY();
                e.consume();
                return;
            }
            activeZone = ResizeZone.NONE;
        });

        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if (resizing && activeZone != ResizeZone.NONE) {
                activeZone.apply(stage,
                    e.getScreenX() - dragX, e.getScreenY() - dragY,
                    dragW, dragH, dragStageX, dragStageY,
                    stage.getMinWidth(), stage.getMinHeight());
                e.consume();
            }
        });

        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            boolean wasResizing = resizing;
            activeZone = ResizeZone.NONE;
            resizing = false;
            clearCursorOverride();
            scene.setCursor(Cursor.DEFAULT);
            if (wasResizing) {
                suppressNextClick = true;
                e.consume();
            }
        });
    }

    private void installDragHandlers(Node dragArea) {
        dragArea.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() != MouseButton.PRIMARY)
                return;
            dragging = true;
            moveOffsetX = e.getScreenX() - stage.getX();
            moveOffsetY = e.getScreenY() - stage.getY();
            e.consume();
        });

        dragArea.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            if (!dragging)
                return;
            if (isSnapped()) {
                double ratio = e.getSceneX() / Math.max(1, scene.getWidth());
                unsnap();
                moveOffsetX = stage.getWidth() * ratio;
                moveOffsetY = e.getSceneY();
            }
            stage.setX(e.getScreenX() - moveOffsetX);
            stage.setY(e.getScreenY() - moveOffsetY);
            e.consume();
        });

        dragArea.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            if (!dragging)
                return;
            applySnap(e.getScreenX(), e.getScreenY());
            dragging = false;
            e.consume();
        });

        dragArea.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (suppressNextClick) {
                suppressNextClick = false;
                e.consume();
                return;
            }
            if (e.getButton() != MouseButton.PRIMARY || e.getClickCount() != 2)
                return;
            toggleMaximize();
            e.consume();
        });
    }

    protected Screen windowScreen() {
        double wx = stage.getX(), wy = stage.getY();
        double wr = wx + stage.getWidth(), wb = wy + stage.getHeight();
        Screen best = Screen.getPrimary();
        double bestArea = 0;

        for (Screen s : Screen.getScreens()) {
            Rectangle2D b = s.getVisualBounds();
            double area = Math.max(0, Math.min(wr, b.getMaxX()) - Math.max(wx, b.getMinX()))
                * Math.max(0, Math.min(wb, b.getMaxY()) - Math.max(wy, b.getMinY()));
            if (area > bestArea) {
                bestArea = area;
                best = s;
            }
        }
        return best;
    }

    private void applySnap(double cursorX, double cursorY) {
        if (isSnapped())
            return;

        Rectangle2D bounds = screenAtCursor(cursorX, cursorY).getVisualBounds();
        boolean left = cursorX <= bounds.getMinX() + SNAP_MARGIN;
        boolean right = cursorX >= bounds.getMaxX() - SNAP_MARGIN;
        boolean top = cursorY <= bounds.getMinY() + SNAP_MARGIN;
        boolean bottom = cursorY >= bounds.getMaxY() - SNAP_MARGIN;

        if (!left && !right && !top && !bottom)
            return;

        double hw = bounds.getWidth() / 2, hh = bounds.getHeight() / 2;
        double bx = bounds.getMinX(), by = bounds.getMinY();

        if (top && !left && !right)
            snapTo(bounds);
        else if (left && top)
            snapTo(new Rectangle2D(bx, by, hw, hh));
        else if (right && top)
            snapTo(new Rectangle2D(bx + hw, by, hw, hh));
        else if (left && bottom)
            snapTo(new Rectangle2D(bx, by + hh, hw, hh));
        else if (right && bottom)
            snapTo(new Rectangle2D(bx + hw, by + hh, hw, hh));
        else if (left)
            snapTo(new Rectangle2D(bx, by, hw, bounds.getHeight()));
        else if (right)
            snapTo(new Rectangle2D(bx + hw, by, hw, bounds.getHeight()));
        else if (bottom)
            snapTo(new Rectangle2D(bx, by + hh, bounds.getWidth(), hh));
    }

    private Screen screenAtCursor(double x, double y) {
        for (Screen s : Screen.getScreens()) {
            Rectangle2D b = s.getVisualBounds();
            if (x >= b.getMinX() && x < b.getMaxX() && y >= b.getMinY() && y < b.getMaxY())
                return s;
        }
        return Screen.getPrimary();
    }

    private void applyCursorOverride(Object target, Cursor cursor) {
        clearCursorOverride();
        if (!(target instanceof Node node))
            return;
        while (node != null && node.getCursor() == null)
            node = node.getParent();
        if (node == null || node.getCursor() == cursor)
            return;
        cursorOverrideNode = node;
        cursorOverrideOriginal = node.getCursor();
        node.setCursor(cursor);
    }

    private void clearCursorOverride() {
        if (cursorOverrideNode == null)
            return;
        cursorOverrideNode.setCursor(cursorOverrideOriginal);
        cursorOverrideNode = null;
        cursorOverrideOriginal = null;
    }

    private ResizeZone detectZone(double x, double y, double w, double h) {
        boolean n = y <= RESIZE_MARGIN, s = y >= h - RESIZE_MARGIN;
        boolean west = x <= RESIZE_MARGIN, east = x >= w - RESIZE_MARGIN;

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

    private enum ResizeZone {
        NONE(Cursor.DEFAULT) {
            @Override
            void resize(Stage s, double dx, double dy, double ow, double oh, double ox, double oy, double mw,
                double mh) {}
        },
        N(Cursor.N_RESIZE) {
            @Override
            void resize(Stage s, double dx, double dy, double ow, double oh, double ox, double oy, double mw,
                double mh) {
                double h = Math.max(mh, oh - dy);
                s.setY(oy + oh - h);
                s.setHeight(h);
            }
        },
        S(Cursor.S_RESIZE) {
            @Override
            void resize(Stage s, double dx, double dy, double ow, double oh, double ox, double oy, double mw,
                double mh) {
                s.setHeight(Math.max(mh, oh + dy));
            }
        },
        W(Cursor.W_RESIZE) {
            @Override
            void resize(Stage s, double dx, double dy, double ow, double oh, double ox, double oy, double mw,
                double mh) {
                double w = Math.max(mw, ow - dx);
                s.setX(ox + ow - w);
                s.setWidth(w);
            }
        },
        E(Cursor.E_RESIZE) {
            @Override
            void resize(Stage s, double dx, double dy, double ow, double oh, double ox, double oy, double mw,
                double mh) {
                s.setWidth(Math.max(mw, ow + dx));
            }
        },
        NW(Cursor.NW_RESIZE) {
            @Override
            void resize(Stage s, double dx, double dy, double ow, double oh, double ox, double oy, double mw,
                double mh) {
                N.resize(s, dx, dy, ow, oh, ox, oy, mw, mh);
                W.resize(s, dx, dy, ow, oh, ox, oy, mw, mh);
            }
        },
        NE(Cursor.NE_RESIZE) {
            @Override
            void resize(Stage s, double dx, double dy, double ow, double oh, double ox, double oy, double mw,
                double mh) {
                N.resize(s, dx, dy, ow, oh, ox, oy, mw, mh);
                E.resize(s, dx, dy, ow, oh, ox, oy, mw, mh);
            }
        },
        SW(Cursor.SW_RESIZE) {
            @Override
            void resize(Stage s, double dx, double dy, double ow, double oh, double ox, double oy, double mw,
                double mh) {
                S.resize(s, dx, dy, ow, oh, ox, oy, mw, mh);
                W.resize(s, dx, dy, ow, oh, ox, oy, mw, mh);
            }
        },
        SE(Cursor.SE_RESIZE) {
            @Override
            void resize(Stage s, double dx, double dy, double ow, double oh, double ox, double oy, double mw,
                double mh) {
                S.resize(s, dx, dy, ow, oh, ox, oy, mw, mh);
                E.resize(s, dx, dy, ow, oh, ox, oy, mw, mh);
            }
        };

        private final Cursor cursor;

        ResizeZone(Cursor cursor) {
            this.cursor = cursor;
        }

        Cursor cursor() {
            return cursor;
        }

        abstract void resize(Stage s, double dx, double dy, double ow, double oh, double ox, double oy, double mw,
            double mh);

        void apply(Stage s, double dx, double dy, double ow, double oh, double ox, double oy, double mw, double mh) {
            resize(s, dx, dy, ow, oh, ox, oy, mw, mh);
        }
    }
}

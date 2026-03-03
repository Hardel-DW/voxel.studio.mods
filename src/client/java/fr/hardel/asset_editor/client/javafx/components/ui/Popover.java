package fr.hardel.asset_editor.client.javafx.components.ui;

import fr.hardel.asset_editor.client.javafx.ResourceLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;
import javafx.stage.Window;
import net.minecraft.resources.Identifier;

public final class Popover extends Popup {

    private static final Identifier SHINE = Identifier.fromNamespaceAndPath("asset_editor", "textures/studio/shine.png");
    private static Popover active;

    private final Node trigger;
    private final StackPane container = new StackPane();

    public Popover(Node trigger, Node content) {
        this.trigger = trigger;
        setAutoHide(false);
        setHideOnEscape(true);

        StackPane inner = new StackPane(content);
        inner.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        try (var stream = ResourceLoader.open(SHINE)) {
            ImageView shine = new ImageView(new Image(stream));
            shine.setPreserveRatio(false);
            shine.setOpacity(0.06);
            shine.setMouseTransparent(true);
            shine.setManaged(false);
            shine.fitWidthProperty().bind(inner.widthProperty());
            shine.fitHeightProperty().bind(inner.heightProperty().multiply(0.35));
            inner.getChildren().addFirst(shine);
        } catch (Exception ignored) {}

        Rectangle innerClip = new Rectangle();
        innerClip.setArcWidth(32);
        innerClip.setArcHeight(32);
        inner.widthProperty().addListener((obs, o, w) -> innerClip.setWidth(w.doubleValue()));
        inner.heightProperty().addListener((obs, o, h) -> innerClip.setHeight(h.doubleValue()));
        inner.setClip(innerClip);

        container.getStyleClass().add("popover");
        container.getChildren().add(inner);
        getContent().add(container);

        setOnHidden(e -> {
            if (active == this) active = null;
        });

        trigger.setOnMouseClicked(e -> {
            e.consume();
            toggle();
        });

        trigger.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null) oldScene.removeEventFilter(MouseEvent.MOUSE_PRESSED, this::onSceneClick);
            if (newScene != null) newScene.addEventFilter(MouseEvent.MOUSE_PRESSED, this::onSceneClick);
        });
    }

    private void onSceneClick(MouseEvent e) {
        if (!isShowing()) return;

        Bounds triggerBounds = trigger.localToScene(trigger.getBoundsInLocal());
        if (triggerBounds.contains(e.getSceneX(), e.getSceneY())) return;

        Point2D popoverScreen = new Point2D(getX(), getY());
        double pw = container.getWidth();
        double ph = container.getHeight();
        Window window = trigger.getScene().getWindow();
        double sx = e.getSceneX() + window.getX() + trigger.getScene().getX();
        double sy = e.getSceneY() + window.getY() + trigger.getScene().getY();
        if (sx >= popoverScreen.getX() && sx <= popoverScreen.getX() + pw
                && sy >= popoverScreen.getY() && sy <= popoverScreen.getY() + ph) return;

        hide();
    }

    public static void hideActive() {
        if (active != null) active.hide();
    }

    public void toggle() {
        if (isShowing()) {
            hide();
            return;
        }
        if (active != null && active != this) active.hide();
        active = this;

        Window window = trigger.getScene().getWindow();
        Bounds bounds = trigger.localToScreen(trigger.getBoundsInLocal());
        if (bounds == null) return;

        show(window, bounds.getMinX(), bounds.getMaxY() + 6);

        double screenBottom = window.getY() + window.getHeight();
        if (getY() + container.getHeight() > screenBottom) {
            setY(bounds.getMinY() - container.getHeight() - 6);
        }
    }
}

package fr.hardel.asset_editor.client.javafx.components.ui;

import fr.hardel.asset_editor.client.javafx.VoxelResourceLoader;
import javafx.geometry.Bounds;
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

    private static final Identifier SHINE = Identifier.fromNamespaceAndPath("asset_editor", "textures/shine.png");
    private static volatile Image shineImage;
    private static Popover active;

    private final Node trigger;
    private final StackPane container = new StackPane();
    private boolean matchTriggerWidth;

    public Popover(Node trigger, Node content) {
        this(trigger, content, false);
    }

    public Popover(Node trigger, Node content, boolean matchTriggerWidth) {
        this.trigger = trigger;
        this.matchTriggerWidth = matchTriggerWidth;
        setAutoHide(false);
        setHideOnEscape(true);

        StackPane inner = new StackPane(content);
        inner.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        Image shine = getShineImage();
        if (shine != null) {
            ImageView shineView = new ImageView(shine);
            shineView.setPreserveRatio(false);
            shineView.setOpacity(0.12);
            shineView.setMouseTransparent(true);
            shineView.setManaged(false);
            shineView.fitWidthProperty().bind(container.widthProperty());
            shineView.fitHeightProperty().bind(container.heightProperty().multiply(0.35));
            inner.getChildren().addFirst(shineView);
        }

        Rectangle clip = new Rectangle();
        clip.setArcWidth(32);
        clip.setArcHeight(32);
        container.widthProperty().addListener((obs, o, w) -> clip.setWidth(w.doubleValue()));
        container.heightProperty().addListener((obs, o, h) -> clip.setHeight(h.doubleValue()));
        container.setClip(clip);

        container.getStyleClass().add("ui-popover");
        container.getChildren().add(inner);
        getContent().add(container);

        setOnHidden(e -> {
            if (active == this)
                active = null;
        });

        trigger.setOnMouseClicked(e -> {
            e.consume();
            toggle();
        });

        trigger.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null)
                oldScene.removeEventFilter(MouseEvent.MOUSE_PRESSED, this::onSceneClick);
            if (newScene != null)
                newScene.addEventFilter(MouseEvent.MOUSE_PRESSED, this::onSceneClick);
        });
    }

    private void onSceneClick(MouseEvent e) {
        if (!isShowing())
            return;
        if (trigger.getScene() == null) {
            hide();
            return;
        }

        Bounds triggerBounds = trigger.localToScene(trigger.getBoundsInLocal());
        if (triggerBounds.contains(e.getSceneX(), e.getSceneY()))
            return;

        Window window = trigger.getScene().getWindow();
        if (window == null) {
            hide();
            return;
        }

        double sx = e.getSceneX() + window.getX() + trigger.getScene().getX();
        double sy = e.getSceneY() + window.getY() + trigger.getScene().getY();
        if (sx >= getX() && sx <= getX() + container.getWidth()
            && sy >= getY() && sy <= getY() + container.getHeight())
            return;

        hide();
    }

    public static void hideActive() {
        if (active != null)
            active.hide();
    }

    public void toggle() {
        if (isShowing()) {
            hide();
            return;
        }
        if (trigger.getScene() == null)
            return;
        if (active != null && active != this)
            active.hide();
        active = this;

        Window window = trigger.getScene().getWindow();
        Bounds bounds = trigger.localToScreen(trigger.getBoundsInLocal());
        if (bounds == null)
            return;

        if (matchTriggerWidth) {
            container.setPrefWidth(bounds.getWidth());
            container.setMinWidth(bounds.getWidth());
            container.setMaxWidth(bounds.getWidth());
        }

        show(window, bounds.getMinX(), bounds.getMaxY() + 8);

        double screenBottom = window.getY() + window.getHeight();
        if (getY() + container.getHeight() > screenBottom) {
            setY(bounds.getMinY() - container.getHeight() - 8);
        }
    }

    private static Image getShineImage() {
        Image current = shineImage;
        if (current != null)
            return current;
        synchronized (Popover.class) {
            if (shineImage != null)
                return shineImage;
            try (var stream = VoxelResourceLoader.open(SHINE)) {
                shineImage = new Image(stream);
            } catch (Exception ignored) {
                shineImage = null;
            }
            return shineImage;
        }
    }
}

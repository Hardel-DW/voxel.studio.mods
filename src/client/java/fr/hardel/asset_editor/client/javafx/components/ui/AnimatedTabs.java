package fr.hardel.asset_editor.client.javafx.components.ui;

import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class AnimatedTabs extends StackPane {

    private final LinkedHashMap<String, Button> buttons = new LinkedHashMap<>();
    private final Rectangle indicator = new Rectangle();
    private final HBox tabsRow = new HBox();
    private final Timeline indicatorTimeline = new Timeline();
    private final SimpleStringProperty activeValue;
    private Consumer<String> onValueChange;

    public AnimatedTabs(LinkedHashMap<String, String> options, String defaultValue, Consumer<String> onChange) {
        this.activeValue = new SimpleStringProperty(defaultValue);
        this.onValueChange = onChange;

        activeValue.addListener((obs, old, val) -> {
            buttons.forEach((k, btn) -> {
                btn.getStyleClass().remove("animated-tabs-button-active");
                if (k.equals(val)) btn.getStyleClass().add("animated-tabs-button-active");
            });
            animateIndicator();
            if (onValueChange != null) onValueChange.accept(val);
        });

        getStyleClass().add("animated-tabs");
        setAlignment(Pos.CENTER_LEFT);
        setMinWidth(Region.USE_PREF_SIZE);
        setMaxWidth(Region.USE_PREF_SIZE);

        indicator.setMouseTransparent(true);
        indicator.setManaged(false);
        indicator.setFill(Color.rgb(255, 255, 255, 0.16));
        indicator.setArcWidth(24);
        indicator.setArcHeight(24);

        tabsRow.setSpacing(0);
        tabsRow.setAlignment(Pos.CENTER_LEFT);
        tabsRow.setPadding(new Insets(4));
        tabsRow.setMinWidth(0);
        tabsRow.setMaxWidth(Region.USE_PREF_SIZE);

        for (Map.Entry<String, String> entry : options.entrySet()) {
            Button btn = buildTabButton(entry.getKey(), entry.getValue());
            buttons.put(entry.getKey(), btn);
            tabsRow.getChildren().add(btn);
            btn.layoutBoundsProperty().addListener((obs, o, b) -> snapIndicator());
        }

        Rectangle clip = new Rectangle();
        clip.setArcWidth(32);
        clip.setArcHeight(32);
        widthProperty().addListener((obs, o, w) -> clip.setWidth(w.doubleValue()));
        heightProperty().addListener((obs, o, h) -> clip.setHeight(h.doubleValue()));
        setClip(clip);

        getChildren().addAll(indicator, tabsRow);
        StackPane.setAlignment(tabsRow, Pos.CENTER_LEFT);

        widthProperty().addListener((obs, o, w) -> snapIndicator());
        heightProperty().addListener((obs, o, h) -> snapIndicator());
        tabsRow.layoutBoundsProperty().addListener((obs, o, b) -> snapIndicator());
        tabsRow.layoutXProperty().addListener((obs, o, x) -> snapIndicator());
        tabsRow.layoutYProperty().addListener((obs, o, y) -> snapIndicator());

        Platform.runLater(() -> Platform.runLater(() -> {
            normalizeButtonWidths();
            snapIndicator();
        }));
    }

    private Button buildTabButton(String value, String label) {
        Button btn = new Button(label);
        btn.getStyleClass().add("animated-tabs-button");
        btn.setFont(VoxelFonts.of(VoxelFonts.Variant.MEDIUM, 14));
        if (value.equals(activeValue.get())) {
            btn.getStyleClass().add("animated-tabs-button-active");
        }
        btn.setMinWidth(0);
        btn.setOnAction(e -> select(value));
        return btn;
    }

    private void normalizeButtonWidths() {
        double maxWidth = 0;
        for (Button button : buttons.values()) {
            maxWidth = Math.max(maxWidth, button.prefWidth(-1));
        }
        if (maxWidth <= 0) return;
        for (Button button : buttons.values()) {
            button.setPrefWidth(maxWidth);
        }
    }

    private void select(String value) {
        if (value.equals(activeValue.get())) {
            snapIndicator();
            return;
        }
        activeValue.set(value);
    }

    private void snapIndicator() {
        Button active = buttons.get(activeValue.get());
        if (active == null) return;
        Bounds bounds = active.getBoundsInParent();
        double x = tabsRow.getLayoutX() + bounds.getMinX();
        double y = tabsRow.getLayoutY() + bounds.getMinY();
        double w = bounds.getWidth() > 0 ? bounds.getWidth() : active.prefWidth(-1);
        double h = bounds.getHeight() > 0 ? bounds.getHeight() : active.prefHeight(-1);
        indicator.setX(x);
        indicator.setY(y);
        indicator.setWidth(w);
        indicator.setHeight(h);
    }

    private void animateIndicator() {
        Button active = buttons.get(activeValue.get());
        if (active == null) return;
        Bounds bounds = active.getBoundsInParent();
        double targetX = tabsRow.getLayoutX() + bounds.getMinX();
        double targetW = bounds.getWidth() > 0 ? bounds.getWidth() : active.prefWidth(-1);

        indicatorTimeline.stop();
        indicatorTimeline.getKeyFrames().setAll(
            new KeyFrame(Duration.millis(300),
                new KeyValue(indicator.xProperty(), targetX, Interpolator.EASE_OUT),
                new KeyValue(indicator.widthProperty(), targetW, Interpolator.EASE_OUT))
        );
        indicatorTimeline.playFromStart();
    }

    public void setValue(String value) { select(value); }
    public String getValue() { return activeValue.get(); }
    public StringProperty valueProperty() { return activeValue; }
}

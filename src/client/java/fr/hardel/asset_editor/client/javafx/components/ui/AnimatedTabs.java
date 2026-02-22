package fr.hardel.asset_editor.client.javafx.components.ui;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Tabs with animated sliding indicator.
 * Container: rounded-2xl bg-zinc-900/70 border border-zinc-800 p-1 overflow-hidden.
 * Indicator: bg-white/10 rounded-xl, slides to active tab (300ms ease-out).
 */
public final class AnimatedTabs extends StackPane {

    private final LinkedHashMap<String, Button> buttons = new LinkedHashMap<>();
    private final Region indicator = new Region();
    private final HBox tabsRow = new HBox();
    private final Timeline indicatorTimeline = new Timeline();
    private String activeValue;
    private Consumer<String> onValueChange;

    public AnimatedTabs(LinkedHashMap<String, String> options, String defaultValue, Consumer<String> onChange) {
        this.activeValue = defaultValue;
        this.onValueChange = onChange;

        getStyleClass().add("animated-tabs");
        setAlignment(Pos.CENTER_LEFT);
        setMinWidth(Region.USE_PREF_SIZE);
        setMaxWidth(Region.USE_PREF_SIZE);

        indicator.getStyleClass().add("animated-tabs-indicator");
        indicator.setMouseTransparent(true);
        indicator.setManaged(false);
        indicator.setPrefHeight(0);

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

        // z-order: indicator behind buttons
        getChildren().addAll(indicator, tabsRow);
        StackPane.setAlignment(tabsRow, Pos.CENTER_LEFT);

        widthProperty().addListener((obs, o, w) -> snapIndicator());
        heightProperty().addListener((obs, o, h) -> snapIndicator());
        tabsRow.layoutBoundsProperty().addListener((obs, o, b) -> snapIndicator());
        tabsRow.layoutXProperty().addListener((obs, o, x) -> snapIndicator());
        tabsRow.layoutYProperty().addListener((obs, o, y) -> snapIndicator());

        Platform.runLater(() -> {
            normalizeButtonWidths();
            snapIndicator();
        });
    }

    private Button buildTabButton(String value, String label) {
        Button btn = new Button(label);
        btn.getStyleClass().add("animated-tabs-button");
        if (value.equals(activeValue)) {
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
        if (value.equals(activeValue)) {
            snapIndicator();
            return;
        }
        activeValue = value;
        buttons.forEach((k, btn) -> {
            btn.getStyleClass().remove("animated-tabs-button-active");
            if (k.equals(activeValue)) btn.getStyleClass().add("animated-tabs-button-active");
        });
        animateIndicator();
        if (onValueChange != null) onValueChange.accept(value);
    }

    private void snapIndicator() {
        Button active = buttons.get(activeValue);
        if (active == null) return;
        Bounds bounds = active.getBoundsInParent();
        double x = tabsRow.getLayoutX() + bounds.getMinX();
        double w = bounds.getWidth();
        double h = Math.max(0, getHeight() - 8);
        indicator.setLayoutX(x);
        indicator.setLayoutY(4);
        indicator.setPrefWidth(w);
        indicator.setPrefHeight(h);
    }

    private void animateIndicator() {
        Button active = buttons.get(activeValue);
        if (active == null) return;
        Bounds bounds = active.getBoundsInParent();
        double targetX = tabsRow.getLayoutX() + bounds.getMinX();
        double targetW = bounds.getWidth();

        indicatorTimeline.stop();
        indicatorTimeline.getKeyFrames().setAll(
            new KeyFrame(Duration.millis(300),
                new KeyValue(indicator.layoutXProperty(), targetX, javafx.animation.Interpolator.EASE_OUT),
                new KeyValue(indicator.prefWidthProperty(), targetW, javafx.animation.Interpolator.EASE_OUT))
        );
        indicatorTimeline.playFromStart();
    }

    public void setValue(String value) { select(value); }
    public String getValue() { return activeValue; }
}

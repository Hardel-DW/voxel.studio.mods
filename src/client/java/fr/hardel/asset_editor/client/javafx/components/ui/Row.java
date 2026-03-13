package fr.hardel.asset_editor.client.javafx.components.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import net.minecraft.client.resources.language.I18n;

public class Row extends HBox {

    private final HBox left = new HBox(16);
    private final VBox main = new VBox(2);
    private final ToggleSwitch toggle = new ToggleSwitch();
    private final javafx.scene.control.Button actionButton = new javafx.scene.control.Button(I18n.get("generic:configure"));
    private Node icon;

    public Row() {
        getStyleClass().add("row");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(16);
        setPadding(new Insets(12));

        main.setMinWidth(0);
        left.setAlignment(Pos.CENTER_LEFT);
        left.setCursor(Cursor.HAND);
        left.getChildren().add(main);
        left.setMinWidth(0);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(main, Priority.ALWAYS);

        toggle.setVisible(false);
        toggle.setManaged(false);

        Region divider = new Region();
        divider.getStyleClass().add("ui-row-divider");
        divider.setMinSize(1, 16);
        divider.setPrefSize(1, 16);
        divider.setMaxSize(1, 16);
        HBox.setMargin(divider, new Insets(0, 8, 0, 8));

        actionButton.getStyleClass().add("ui-row-action");
        actionButton.setCursor(Cursor.HAND);

        HBox right = new HBox(8, toggle, divider, actionButton);
        right.setAlignment(Pos.CENTER_RIGHT);

        getChildren().addAll(left, right);
    }

    public Row setIcon(Node icon) {
        if (this.icon != null)
            left.getChildren().remove(this.icon);
        this.icon = icon;
        if (icon != null)
            left.getChildren().addFirst(icon);
        return this;
    }

    public Row setContent(Node... nodes) {
        main.getChildren().setAll(nodes);
        return this;
    }

    public Row setOnClick(Runnable handler) {
        left.setOnMouseClicked(e -> handler.run());
        return this;
    }

    public ToggleSwitch toggle() {
        toggle.setVisible(true);
        toggle.setManaged(true);
        return toggle;
    }

    public Row setActionText(String key) {
        actionButton.setText(I18n.get(key));
        return this;
    }

    public Row setOnAction(Runnable handler) {
        actionButton.setOnAction(e -> handler.run());
        return this;
    }
}

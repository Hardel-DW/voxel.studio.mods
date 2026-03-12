package fr.hardel.asset_editor.client.javafx.components.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import net.minecraft.client.resources.language.I18n;

public class Row extends HBox {

    private final VBox main = new VBox(2);
    private final ToggleSwitch toggle = new ToggleSwitch();
    private final Label actionLabel = new Label(I18n.get("generic:configure"));
    private Node icon;

    public Row() {
        getStyleClass().add("row");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(16);
        setPadding(new Insets(12));
        setCursor(Cursor.HAND);

        main.setMinWidth(0);
        HBox.setHgrow(main, Priority.ALWAYS);

        toggle.setVisible(false);
        toggle.setManaged(false);
        toggle.addEventFilter(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);

        Region divider = new Region();
        divider.getStyleClass().add("row-divider");
        divider.setMinSize(1, 16);
        divider.setPrefSize(1, 16);
        divider.setMaxSize(1, 16);
        HBox.setMargin(divider, new Insets(0, 8, 0, 8));

        actionLabel.getStyleClass().add("row-action");
        actionLabel.addEventFilter(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);

        HBox right = new HBox(8, toggle, divider, actionLabel);
        right.setAlignment(Pos.CENTER_RIGHT);

        getChildren().addAll(main, right);
    }

    public Row setIcon(Node icon) {
        if (this.icon != null)
            getChildren().remove(this.icon);
        this.icon = icon;
        if (icon != null)
            getChildren().addFirst(icon);
        return this;
    }

    public Row setContent(Node... nodes) {
        main.getChildren().setAll(nodes);
        return this;
    }

    public Row setOnClick(Runnable handler) {
        setOnMouseClicked(e -> handler.run());
        return this;
    }

    public ToggleSwitch toggle() {
        toggle.setVisible(true);
        toggle.setManaged(true);
        return toggle;
    }

    public Row setActionText(String key) {
        actionLabel.setText(I18n.get(key));
        return this;
    }

    public Row setOnAction(Runnable handler) {
        actionLabel.setOnMouseClicked(e -> handler.run());
        return this;
    }
}

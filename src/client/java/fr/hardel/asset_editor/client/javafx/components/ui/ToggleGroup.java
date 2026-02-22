package fr.hardel.asset_editor.client.javafx.components.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ToggleGroup extends HBox {

    private final Supplier<String> valueSupplier;
    private final Consumer<String> onChange;
    private final HashMap<String, Button> options = new HashMap<>();

    public ToggleGroup(Supplier<String> valueSupplier, Consumer<String> onChange) {
        this.valueSupplier = valueSupplier;
        this.onChange = onChange;
        getStyleClass().add("toggle-group");
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(4));
        setSpacing(0);
    }

    public void addOption(String value, String label) {
        Button button = new Button(label);
        button.getStyleClass().add("toggle-group-option");
        button.setOnAction(event -> {
            onChange.accept(value);
            refresh();
        });
        options.put(value, button);
        getChildren().add(button);
        refresh();
    }

    public void refresh() {
        String active = valueSupplier.get();
        for (Map.Entry<String, Button> option : options.entrySet()) {
            Button button = option.getValue();
            button.getStyleClass().remove("toggle-group-option-active");
            if (option.getKey().equals(active)) {
                button.getStyleClass().add("toggle-group-option-active");
            }
        }
    }
}

package fr.hardel.asset_editor.client.javafx.components.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ToggleGroup extends HBox {

    private final Supplier<String> valueSupplier;
    private final Consumer<String> onChange;
    private final LinkedHashMap<String, ToggleOption> options = new LinkedHashMap<>();

    public ToggleGroup(Supplier<String> valueSupplier, Consumer<String> onChange) {
        this.valueSupplier = valueSupplier;
        this.onChange = onChange;
        getStyleClass().add("toggle-group");
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(4));
        setSpacing(0);
    }

    public void addOption(String value, String label) {
        Button button = new Button(label == null ? "" : label.toUpperCase(Locale.ROOT));
        button.getStyleClass().add("toggle-group-option");
        button.setAlignment(Pos.CENTER);
        button.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(button, Priority.ALWAYS);
        button.setOnAction(event -> {
            onChange.accept(value);
            refresh();
        });
        options.put(value, new ToggleOption(button, null));
        getChildren().add(button);
        refresh();
    }

    public void addIconOption(String value, Identifier iconPath) {
        SvgIcon icon = new SvgIcon(iconPath, 16, Color.WHITE);
        icon.setOpacity(0.65);

        StackPane graphic = new StackPane(icon);
        graphic.setPrefSize(16, 16);
        graphic.setMinSize(16, 16);
        graphic.setMaxSize(16, 16);

        Button button = new Button();
        button.setGraphic(graphic);
        button.getStyleClass().add("toggle-group-option");
        button.getStyleClass().add("toggle-group-option-icon");
        button.setAlignment(Pos.CENTER);
        button.setOnAction(event -> {
            onChange.accept(value);
            refresh();
        });

        options.put(value, new ToggleOption(button, icon));
        getChildren().add(button);
        refresh();
    }

    public void refresh() {
        String active = valueSupplier.get();
        for (Map.Entry<String, ToggleOption> option : options.entrySet()) {
            ToggleOption toggleOption = option.getValue();
            Button button = toggleOption.button();
            button.getStyleClass().remove("toggle-group-option-active");
            if (toggleOption.icon() != null) {
                toggleOption.icon().setOpacity(0.65);
            }

            if (option.getKey().equals(active)) {
                button.getStyleClass().add("toggle-group-option-active");
                if (toggleOption.icon() != null) {
                    toggleOption.icon().setOpacity(1.0);
                }
            }
        }
    }

    private record ToggleOption(Button button, SvgIcon icon) {
    }
}

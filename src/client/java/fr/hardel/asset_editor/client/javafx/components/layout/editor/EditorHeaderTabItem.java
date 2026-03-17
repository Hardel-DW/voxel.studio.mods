package fr.hardel.asset_editor.client.javafx.components.layout.editor;

import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

public final class EditorHeaderTabItem extends Button {

    public EditorHeaderTabItem(String label, boolean active, boolean disabled, Runnable onClick, String disabledTooltip) {
        super(label);
        getStyleClass().add("editor-header-tab-button");
        if (active) getStyleClass().add("editor-header-tab-button-active");
        if (disabled) {
            getStyleClass().add("editor-header-tab-button-disabled");
            setDisable(true);
            if (disabledTooltip != null) {
                Tooltip tip = new Tooltip(disabledTooltip);
                tip.setShowDelay(Duration.millis(200));
                setTooltip(tip);
            }
        } else {
            setOnAction(event -> onClick.run());
        }
    }
}

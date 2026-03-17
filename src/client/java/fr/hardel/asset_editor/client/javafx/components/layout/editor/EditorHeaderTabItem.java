package fr.hardel.asset_editor.client.javafx.components.layout.editor;

import javafx.scene.control.Button;

public final class EditorHeaderTabItem extends Button {

    public EditorHeaderTabItem(String label, boolean active, Runnable onClick) {
        super(label);
        getStyleClass().add("editor-header-tab-button");
        if (active) getStyleClass().add("editor-header-tab-button-active");
        setOnAction(event -> onClick.run());
    }
}

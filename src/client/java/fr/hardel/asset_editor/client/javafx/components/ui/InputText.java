package fr.hardel.asset_editor.client.javafx.components.ui;

import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public final class TextInput extends HBox {

    private final TextField field = new TextField();

    public TextInput(String promptKey) {
        getStyleClass().add("text-input");
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(0, 12, 0, 12));
        setMinHeight(40);
        setPrefHeight(40);
        setMaxHeight(40);

        field.setPromptText(net.minecraft.client.resources.language.I18n.get(promptKey));
        field.getStyleClass().add("text-input-field");
        field.setFont(VoxelFonts.rubik(VoxelFonts.Rubik.REGULAR, 13));
        HBox.setHgrow(field, Priority.ALWAYS);

        getChildren().add(field);
    }

    public TextField field() {
        return field;
    }

    public String getText() {
        return field.getText();
    }

    public void setText(String text) {
        field.setText(text);
    }
}

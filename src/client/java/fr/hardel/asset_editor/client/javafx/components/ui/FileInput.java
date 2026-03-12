package fr.hardel.asset_editor.client.javafx.components.ui;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import net.minecraft.client.resources.language.I18n;

import java.io.File;
import java.util.function.Consumer;

public final class FileInput extends VBox {

    private final Consumer<File> onFileSelected;
    private final Label fileLabel;

    public FileInput(String promptKey, String accept, Consumer<File> onFileSelected) {
        this.onFileSelected = onFileSelected;
        getStyleClass().add("ui-file-input");
        setAlignment(Pos.CENTER);
        setMinHeight(80);
        setPrefHeight(80);
        setCursor(Cursor.HAND);
        setSpacing(4);

        fileLabel = new Label(I18n.get(promptKey));
        fileLabel.setTextFill(VoxelColors.ZINC_500);
        fileLabel.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 12));

        getChildren().add(fileLabel);

        setOnMouseClicked(e -> openFileChooser(accept));

        setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) {
                e.acceptTransferModes(TransferMode.COPY);
                getStyleClass().add("ui-file-input-drag");
            }
            e.consume();
        });

        setOnDragExited(e -> {
            getStyleClass().remove("ui-file-input-drag");
            e.consume();
        });

        setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            getStyleClass().remove("ui-file-input-drag");
            if (db.hasFiles()) {
                File file = db.getFiles().getFirst();
                acceptFile(file);
            }
            e.setDropCompleted(true);
            e.consume();
        });
    }

    private void openFileChooser(String accept) {
        FileChooser chooser = new FileChooser();
        if (accept != null && !accept.isEmpty()) {
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Files", accept));
        }
        File file = chooser.showOpenDialog(getScene().getWindow());
        if (file != null) acceptFile(file);
    }

    private void acceptFile(File file) {
        fileLabel.setText(file.getName());
        fileLabel.setTextFill(VoxelColors.ZINC_200);
        if (onFileSelected != null) onFileSelected.accept(file);
    }
}

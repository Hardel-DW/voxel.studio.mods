package fr.hardel.asset_editor.client.javafx.components.ui;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Category separator: zinc-700 line | title (text-2xl semibold) | zinc-700 line.
 * Children in a VBox below with gap-4.
 */
public class Category extends VBox {

    private final VBox childrenBox = new VBox(16);

    public Category(String title) {
        setSpacing(32);
        setMaxWidth(Double.MAX_VALUE);

        Region leftLine = new Region();
        leftLine.setPrefHeight(4);
        leftLine.setMinHeight(4);
        leftLine.setMaxHeight(4);
        leftLine.setBackground(new Background(
            new BackgroundFill(VoxelColors.ZINC_700, null, null)));
        HBox.setHgrow(leftLine, Priority.ALWAYS);

        Label titleLabel = new Label(title);
        titleLabel.setFont(VoxelFonts.of(VoxelFonts.Variant.SEMI_BOLD, 24));
        titleLabel.setTextFill(VoxelColors.ZINC_100);
        titleLabel.setPadding(new Insets(0, 16, 0, 16));

        Region rightLine = new Region();
        rightLine.setPrefHeight(4);
        rightLine.setMinHeight(4);
        rightLine.setMaxHeight(4);
        rightLine.setBackground(new Background(
            new BackgroundFill(VoxelColors.ZINC_700, null, null)));
        HBox.setHgrow(rightLine, Priority.ALWAYS);

        HBox separator = new HBox();
        separator.setAlignment(Pos.CENTER);
        separator.getChildren().addAll(leftLine, titleLabel, rightLine);
        separator.setMaxWidth(Double.MAX_VALUE);

        getChildren().addAll(separator, childrenBox);
    }

    public void addContent(Node... nodes) {
        childrenBox.getChildren().addAll(nodes);
    }

    public VBox getChildrenBox() { return childrenBox; }
}

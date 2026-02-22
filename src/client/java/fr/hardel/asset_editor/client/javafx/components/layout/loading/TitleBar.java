package fr.hardel.asset_editor.client.javafx.components.layout.loading;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.components.ui.SvgIcon;
import fr.hardel.asset_editor.client.javafx.components.ui.WindowControls;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

/**
 * Custom undecorated title bar matching TitleBar.tsx.
 * Handles window drag, minimize, maximize and close.
 */
public final class TitleBar extends VBox {

    private static final Identifier LOGO = Identifier.fromNamespaceAndPath("asset_editor", "icons/logo.svg");

    private double dragOffsetX, dragOffsetY;

    public TitleBar(Stage stage) {
        getChildren().addAll(buildBar(stage), buildSeparator());
    }

    private HBox buildBar(Stage stage) {
        HBox bar = new HBox();
        bar.setPrefHeight(32);
        bar.setMinHeight(32);
        bar.setMaxHeight(32);
        bar.setStyle("-fx-background-color: transparent;");

        HBox left = buildLeft();
        Region drag = new Region();
        HBox.setHgrow(drag, Priority.ALWAYS);
        HBox controls = buildControls(stage);

        bar.setOnMousePressed(e -> {
            dragOffsetX = e.getScreenX() - stage.getX();
            dragOffsetY = e.getScreenY() - stage.getY();
        });
        bar.setOnMouseDragged(e -> {
            if (!stage.isMaximized()) {
                stage.setX(e.getScreenX() - dragOffsetX);
                stage.setY(e.getScreenY() - dragOffsetY);
            }
        });
        bar.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) stage.setMaximized(!stage.isMaximized());
        });

        bar.getChildren().addAll(left, drag, controls);
        return bar;
    }

    private HBox buildLeft() {
        SvgIcon logo = new SvgIcon(LOGO, 16, Color.WHITE);

        Text title = new Text(I18n.get("tauri:app.title"));
        title.getStyleClass().add("title-bar-logo-label");
        title.setFill(VoxelColors.ZINC_400);

        HBox left = new HBox(8, logo, title);
        left.setAlignment(Pos.CENTER_LEFT);
        left.setPadding(new Insets(0, 0, 0, 12));
        left.setMouseTransparent(true);
        return left;
    }

    private HBox buildControls(Stage stage) {
        HBox controls = new WindowControls(stage, "title-bar-button", 36, 32, "-fx-background-color: transparent;",
                stage::close);
        controls.setAlignment(Pos.CENTER_RIGHT);
        return controls;
    }

    private Region buildSeparator() {
        Region sep = new Region();
        sep.getStyleClass().add("title-separator");
        return sep;
    }
}




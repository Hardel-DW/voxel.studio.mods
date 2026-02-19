package fr.hardel.asset_editor.client.javafx.layout;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.ui.SvgIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.SVGPath;
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
        HBox controls = new HBox();
        controls.setAlignment(Pos.CENTER_RIGHT);

        StackPane minimize = windowButton("M0 0h10v1H0z", 10, 1, VoxelColors.ZINC_200);
        minimize.setOnMouseClicked(e -> { stage.setIconified(true); e.consume(); });

        StackPane maximize = windowButton("M0 0v10h10V0H0zm1 1h8v8H1V1z", 10, 10, VoxelColors.ZINC_200);
        maximize.setOnMouseClicked(e -> { stage.setMaximized(!stage.isMaximized()); e.consume(); });

        StackPane close = windowButton(
                "M1.41 0L0 1.41 3.59 5 0 8.59 1.41 10 5 6.41 8.59 10 10 8.59 6.41 5 10 1.41 8.59 0 5 3.59 1.41 0z",
                10, 10, VoxelColors.RED_400);
        close.setOnMouseClicked(e -> { stage.close(); e.consume(); });

        controls.getChildren().addAll(minimize, maximize, close);
        return controls;
    }

    private StackPane windowButton(String pathData, double vbW, double vbH, Color hoverFill) {
        SVGPath icon = new SVGPath();
        icon.setContent(pathData);
        icon.setFill(VoxelColors.ZINC_500);

        double scale = 12.0 / Math.max(vbW, vbH);
        icon.setScaleX(scale);
        icon.setScaleY(scale);

        StackPane button = new StackPane(icon);
        button.getStyleClass().add("title-bar-button");
        button.setPrefHeight(32);
        button.setMinHeight(32);
        button.setMaxHeight(32);
        button.setPrefWidth(36);
        button.setAlignment(Pos.CENTER);
        button.setCursor(Cursor.HAND);
        button.setStyle("-fx-background-color: transparent;");

        button.setOnMouseEntered(e -> icon.setFill(hoverFill));
        button.setOnMouseExited(e -> icon.setFill(VoxelColors.ZINC_500));
        return button;
    }

    private Region buildSeparator() {
        Region sep = new Region();
        sep.getStyleClass().add("title-separator");
        sep.setPrefHeight(1);
        sep.setMinHeight(1);
        sep.setMaxHeight(1);
        sep.setBackground(new Background(new BackgroundFill(
                new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                        new Stop(0, VoxelColors.ZINC_900),
                        new Stop(0.5, VoxelColors.ZINC_800),
                        new Stop(1, VoxelColors.ZINC_900)),
                null, null)));
        sep.setOpacity(0.25);
        return sep;
    }
}

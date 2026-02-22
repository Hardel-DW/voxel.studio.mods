package fr.hardel.asset_editor.client.javafx.components.ui;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;

public final class WindowControls extends HBox {

    private static final String MINIMIZE_PATH = "M0 0h10v1H0z";
    private static final String MAXIMIZE_PATH = "M0 0v10h10V0H0zm1 1h8v8H1V1z";
    private static final String CLOSE_PATH =
            "M1.41 0L0 1.41 3.59 5 0 8.59 1.41 10 5 6.41 8.59 10 10 8.59 6.41 5 10 1.41 8.59 0 5 3.59 1.41 0z";

    public WindowControls(Stage stage, String buttonStyleClass, double buttonWidth, double buttonHeight,
            String inlineButtonStyle, Runnable closeAction) {
        StackPane minimize = button(buttonStyleClass, MINIMIZE_PATH, 10, 1, buttonWidth, buttonHeight,
                inlineButtonStyle, VoxelColors.ZINC_200);
        minimize.setOnMouseClicked(e -> {
            stage.setIconified(true);
            e.consume();
        });

        StackPane maximize = button(buttonStyleClass, MAXIMIZE_PATH, 10, 10, buttonWidth, buttonHeight,
                inlineButtonStyle, VoxelColors.ZINC_200);
        maximize.setOnMouseClicked(e -> {
            stage.setMaximized(!stage.isMaximized());
            e.consume();
        });

        StackPane close = button(buttonStyleClass, CLOSE_PATH, 10, 10, buttonWidth, buttonHeight,
                inlineButtonStyle, VoxelColors.RED_400);
        close.setOnMouseClicked(e -> {
            closeAction.run();
            e.consume();
        });

        setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(minimize, maximize, close);
    }

    private StackPane button(String styleClass, String pathData, double vbW, double vbH,
            double buttonWidth, double buttonHeight, String inlineStyle, Color hoverFill) {
        SVGPath icon = new SVGPath();
        icon.setContent(pathData);
        icon.setFill(VoxelColors.ZINC_500);

        double scale = 12.0 / Math.max(vbW, vbH);
        icon.setScaleX(scale);
        icon.setScaleY(scale);

        StackPane button = new StackPane(icon);
        button.getStyleClass().add(styleClass);
        button.setPrefSize(buttonWidth, buttonHeight);
        button.setMinSize(buttonWidth, buttonHeight);
        button.setMaxSize(buttonWidth, buttonHeight);
        button.setAlignment(Pos.CENTER);
        button.setCursor(Cursor.HAND);
        if (inlineStyle != null && !inlineStyle.isBlank())
            button.setStyle(inlineStyle);

        button.setOnMouseEntered(e -> icon.setFill(hoverFill));
        button.setOnMouseExited(e -> icon.setFill(VoxelColors.ZINC_500));
        return button;
    }
}

package fr.hardel.asset_editor.client.javafx.components.ui;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import net.minecraft.resources.Identifier;

public final class WindowControls extends HBox {

    private static final Identifier MINIMIZE_ICON = Identifier.fromNamespaceAndPath("asset_editor", "icons/window/minimize.svg");
    private static final Identifier MAXIMIZE_ICON = Identifier.fromNamespaceAndPath("asset_editor", "icons/window/maximize.svg");
    private static final Identifier CLOSE_ICON = Identifier.fromNamespaceAndPath("asset_editor", "icons/window/close.svg");

    public WindowControls(Stage stage, String buttonStyleClass, double buttonWidth, double buttonHeight,
            String inlineButtonStyle, Runnable closeAction) {
        StackPane minimize = button(buttonStyleClass, MINIMIZE_ICON, buttonWidth, buttonHeight,
                inlineButtonStyle, VoxelColors.ZINC_200);
        minimize.setOnMouseClicked(e -> {
            stage.setIconified(true);
            e.consume();
        });

        StackPane maximize = button(buttonStyleClass, MAXIMIZE_ICON, buttonWidth, buttonHeight,
                inlineButtonStyle, VoxelColors.ZINC_200);
        maximize.setOnMouseClicked(e -> {
            stage.setMaximized(!stage.isMaximized());
            e.consume();
        });

        StackPane close = button(buttonStyleClass, CLOSE_ICON, buttonWidth, buttonHeight,
                inlineButtonStyle, VoxelColors.RED_400);
        close.setOnMouseClicked(e -> {
            closeAction.run();
            e.consume();
        });

        setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(minimize, maximize, close);
    }

    private StackPane button(String styleClass, Identifier iconPath,
            double buttonWidth, double buttonHeight, String inlineStyle, Color hoverFill) {
        SvgIcon icon = new SvgIcon(iconPath, 12, VoxelColors.ZINC_500);

        StackPane button = new StackPane(icon);
        button.getStyleClass().add(styleClass);
        button.setPrefSize(buttonWidth, buttonHeight);
        button.setMinSize(buttonWidth, buttonHeight);
        button.setMaxSize(buttonWidth, buttonHeight);
        button.setAlignment(Pos.CENTER);
        button.setCursor(Cursor.HAND);
        if (inlineStyle != null && !inlineStyle.isBlank())
            button.setStyle(inlineStyle);

        button.setOnMouseEntered(e -> icon.setIconFill(hoverFill));
        button.setOnMouseExited(e -> icon.setIconFill(VoxelColors.ZINC_500));
        return button;
    }
}

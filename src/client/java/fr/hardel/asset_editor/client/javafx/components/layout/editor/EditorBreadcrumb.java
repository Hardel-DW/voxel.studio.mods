package fr.hardel.asset_editor.client.javafx.components.layout.editor;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.components.ui.SvgIcon;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Locale;

public final class EditorBreadcrumb extends HBox {

    private static final Identifier BACK_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/back.svg");

    public EditorBreadcrumb(String rootLabel, List<String> segments, boolean showBack, Runnable onBack) {
        getStyleClass().add("editor-breadcrumb");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(8);

        if (showBack && onBack != null) {
            SvgIcon back = new SvgIcon(BACK_ICON, 14, VoxelColors.ZINC_400);
            back.setOpacity(0.5);

            Label backLabel = new Label(I18n.get("generic:back"));
            backLabel.getStyleClass().add("editor-breadcrumb-back-label");

            HBox backBtn = new HBox(6, back, backLabel);
            backBtn.getStyleClass().add("editor-breadcrumb-back");
            backBtn.setAlignment(Pos.CENTER_LEFT);
            backBtn.setCursor(Cursor.HAND);
            backBtn.setOnMouseEntered(event -> {
                back.setOpacity(1.0);
                backLabel.setStyle("-fx-text-fill: white;");
            });
            backBtn.setOnMouseExited(event -> {
                back.setOpacity(0.5);
                backLabel.setStyle("");
            });
            backBtn.setOnMouseClicked(event -> onBack.run());
            getChildren().add(backBtn);
        }

        Label root = new Label(rootLabel.toUpperCase(Locale.ROOT));
        root.getStyleClass().add("editor-breadcrumb-root");
        getChildren().add(root);

        for (int i = 0; i < segments.size(); i++) {
            Label separator = new Label("/");
            separator.getStyleClass().add("editor-breadcrumb-separator");

            Label segment = new Label(segments.get(i).toUpperCase(Locale.ROOT));
            segment.getStyleClass().add(i == segments.size() - 1
                ? "editor-breadcrumb-leaf"
                : "editor-breadcrumb-segment");
            getChildren().addAll(separator, segment);
        }
    }
}

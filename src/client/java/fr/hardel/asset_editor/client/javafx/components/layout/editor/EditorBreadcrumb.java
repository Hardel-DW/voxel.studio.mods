package fr.hardel.asset_editor.client.javafx.components.layout.editor;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.components.ui.SvgIcon;
import fr.hardel.asset_editor.client.javafx.lib.utils.TextUtils;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class EditorBreadcrumb extends HBox {

    private static final Identifier BACK_ICON = Identifier.fromNamespaceAndPath("asset_editor", "icons/back.svg");

    public EditorBreadcrumb(String rootLabel, String filterPath, String elementId, boolean isOverview, Runnable onBack) {
        getStyleClass().add("editor-breadcrumb");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(2);

        if (!isOverview && onBack != null) {
            SvgIcon back = new SvgIcon(BACK_ICON, 14, VoxelColors.ZINC_400);
            back.setOpacity(0.5);

            Label backLabel = new Label(I18n.get("back"));
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

        List<String> segments = buildSegments(filterPath, elementId, isOverview);
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

    private static List<String> buildSegments(String filterPath, String elementId, boolean isOverview) {
        ArrayList<String> segments = new ArrayList<>();
        if (isOverview) {
            if (filterPath == null || filterPath.isBlank()) return segments;
            String[] parts = filterPath.split("/");
            for (String part : parts) {
                segments.add(TextUtils.toDisplay(part));
            }
            return segments;
        }

        if (elementId == null || elementId.isBlank()) return segments;
        String clean = elementId.contains("$") ? elementId.substring(0, elementId.indexOf('$')) : elementId;
        int separator = clean.indexOf(':');
        if (separator < 0) {
            segments.add(TextUtils.toDisplay(clean));
            return segments;
        }

        segments.add(clean.substring(0, separator));
        String resource = clean.substring(separator + 1);
        if (resource.isBlank()) return segments;
        String[] parts = resource.split("/");
        for (String part : parts) {
            segments.add(TextUtils.toDisplay(part));
        }
        return segments;
    }
}

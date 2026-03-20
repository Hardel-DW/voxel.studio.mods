package fr.hardel.asset_editor.client.javafx.components.layout.editor;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.components.ui.SvgIcon;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

public final class NoPermissionPage extends VBox {

    private static final Identifier LOCK_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/lock.svg");

    public NoPermissionPage() {
        setAlignment(Pos.CENTER);
        setSpacing(16);
        getStyleClass().add("studio-root");

        SvgIcon icon = new SvgIcon(LOCK_ICON, 48, VoxelColors.ZINC_500);
        icon.setOpacity(0.5);

        Label title = new Label(I18n.get("studio:permission.no_access.title"));
        title.setStyle("-fx-text-fill: #a1a1aa; -fx-font-size: 20; -fx-font-weight: 700;");

        Label description = new Label(I18n.get("studio:permission.no_access.description"));
        description.setStyle("-fx-text-fill: #71717a; -fx-font-size: 14;");
        description.setWrapText(true);
        description.setMaxWidth(400);
        description.setAlignment(Pos.CENTER);

        getChildren().addAll(icon, title, description);
    }
}

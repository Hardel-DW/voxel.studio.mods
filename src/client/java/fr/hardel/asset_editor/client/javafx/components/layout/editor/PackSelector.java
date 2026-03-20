package fr.hardel.asset_editor.client.javafx.components.layout.editor;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.components.ui.Popover;
import fr.hardel.asset_editor.client.javafx.components.ui.SvgIcon;
import fr.hardel.asset_editor.client.javafx.lib.FxSelectionBindings;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.state.ClientPackInfo;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

public final class PackSelector extends StackPane {

    private static final Identifier FOLDER_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/folder.svg");
    private static final Identifier CHEVRON_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/chevron-down.svg");
    private static final Identifier PENCIL_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/pencil.svg");

    private final StudioContext context;
    private final FxSelectionBindings bindings = new FxSelectionBindings();
    private final Label nameLabel = new Label();
    private final VBox popoverContent = new VBox(4);

    public PackSelector(StudioContext context) {
        this.context = context;
        getStyleClass().add("pack-selector");
        setCursor(Cursor.HAND);
        setPrefWidth(280);
        setMaxWidth(280);
        setMinWidth(280);
        setPrefHeight(36);
        setMinHeight(36);
        setMaxHeight(36);

        SvgIcon folder = new SvgIcon(FOLDER_ICON, 14, VoxelColors.ZINC_400);
        SvgIcon chevron = new SvgIcon(CHEVRON_ICON, 10, VoxelColors.ZINC_500);

        nameLabel.getStyleClass().add("pack-selector-label");
        nameLabel.setFont(VoxelFonts.of(VoxelFonts.Variant.MEDIUM, 12));
        nameLabel.setMaxWidth(200);
        nameLabel.setEllipsisString("...");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(8, folder, nameLabel, spacer, chevron);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(0, 10, 0, 10));
        row.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        Rectangle clip = new Rectangle();
        clip.setArcWidth(16);
        clip.setArcHeight(16);
        widthProperty().addListener((obs, o, w) -> clip.setWidth(w.doubleValue()));
        heightProperty().addListener((obs, o, h) -> clip.setHeight(h.doubleValue()));
        setClip(clip);

        getChildren().add(row);

        new Popover(this, buildPopoverContent(), true);

        bindings.observe(context.selectSelectedPack(), selectedPack -> {
            refreshLabel();
            refreshPopoverContent();
        });
        bindings.observe(context.selectAvailablePacks(), packs -> refreshPopoverContent());

        setOnMouseEntered(e -> {
            if (!getStyleClass().contains("pack-selector-hover"))
                getStyleClass().add("pack-selector-hover");
        });
        setOnMouseExited(e -> getStyleClass().remove("pack-selector-hover"));
    }

    private void refreshLabel() {
        ClientPackInfo pack = context.packState().selectedPack();
        if (pack == null) {
            nameLabel.setText(I18n.get("studio:pack.none"));
            nameLabel.setTextFill(VoxelColors.ZINC_500);
            return;
        }
        nameLabel.setText(pack.name());
        nameLabel.setTextFill(VoxelColors.ZINC_200);
    }

    private VBox buildPopoverContent() {
        popoverContent.setMaxWidth(Double.MAX_VALUE);
        refreshPopoverContent();
        return popoverContent;
    }

    private void refreshPopoverContent() {
        popoverContent.getChildren().clear();

        Label header = new Label(I18n.get("studio:pack.select"));
        header.setTextFill(VoxelColors.ZINC_300);
        header.setFont(VoxelFonts.of(VoxelFonts.Variant.SEMI_BOLD, 13));
        header.setPadding(new Insets(0, 0, 4, 0));
        popoverContent.getChildren().add(header);

        ClientPackInfo selected = context.packState().selectedPack();

        for (ClientPackInfo pack : context.packState().availablePacks()) {
            boolean isSelected = pack.equals(selected);
            popoverContent.getChildren().add(buildPackRow(pack, isSelected));
        }

        Region separator = new Region();
        separator.getStyleClass().add("pack-popover-separator");
        separator.setPrefHeight(1);
        separator.setMaxHeight(1);
        VBox.setMargin(separator, new Insets(6, 0, 6, 0));

        popoverContent.getChildren().addAll(separator, buildCreateButton());
    }

    private HBox buildPackRow(ClientPackInfo pack, boolean isSelected) {
        SvgIcon icon = new SvgIcon(FOLDER_ICON, 14, isSelected ? VoxelColors.ZINC_200 : VoxelColors.ZINC_400);

        Label name = new Label(pack.name());
        name.setTextFill(isSelected ? VoxelColors.ZINC_100 : VoxelColors.ZINC_300);
        name.setFont(VoxelFonts.of(isSelected ? VoxelFonts.Variant.SEMI_BOLD : VoxelFonts.Variant.MEDIUM, 13));

        HBox row = new HBox(10, icon, name);
        row.getStyleClass().add(isSelected ? "pack-popover-row-active" : "pack-popover-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 12, 8, 12));
        row.setCursor(Cursor.HAND);

        if (!isSelected) {
            row.setOnMouseEntered(e -> {
                if (!row.getStyleClass().contains("pack-popover-row-hover"))
                    row.getStyleClass().add("pack-popover-row-hover");
            });
            row.setOnMouseExited(e -> row.getStyleClass().remove("pack-popover-row-hover"));
        }

        row.setOnMouseClicked(e -> {
            e.consume();
            context.packState().selectPack(pack);
        });

        return row;
    }

    private HBox buildCreateButton() {
        SvgIcon icon = new SvgIcon(PENCIL_ICON, 12, VoxelColors.ZINC_400);
        Label label = new Label(I18n.get("studio:pack.create"));
        label.setTextFill(VoxelColors.ZINC_300);
        label.setFont(VoxelFonts.of(VoxelFonts.Variant.MEDIUM, 12));

        HBox btn = new HBox(8, icon, label);
        btn.getStyleClass().add("pack-popover-create");
        btn.setAlignment(Pos.CENTER);
        btn.setPadding(new Insets(8, 12, 8, 12));
        btn.setCursor(Cursor.HAND);

        btn.setOnMouseEntered(e -> {
            if (!btn.getStyleClass().contains("pack-popover-create-hover"))
                btn.getStyleClass().add("pack-popover-create-hover");
        });
        btn.setOnMouseExited(e -> btn.getStyleClass().remove("pack-popover-create-hover"));
        btn.setOnMouseClicked(e -> {
            e.consume();
            Popover.hideActive();
            var dialog = PackCreateDialog.create(context);
            dialog.show(getScene().getWindow());
        });

        return btn;
    }
}

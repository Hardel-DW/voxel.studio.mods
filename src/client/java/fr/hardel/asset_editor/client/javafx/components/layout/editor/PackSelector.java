package fr.hardel.asset_editor.client.javafx.components.layout.editor;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.components.ui.Popover;
import fr.hardel.asset_editor.client.javafx.components.ui.SvgIcon;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.store.StudioPackState.PackInfo;
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

    private static final Identifier FOLDER_ICON = Identifier.fromNamespaceAndPath("asset_editor", "icons/folder.svg");
    private static final Identifier CHEVRON_ICON = Identifier.fromNamespaceAndPath("asset_editor", "icons/chevron-down.svg");
    private static final Identifier PENCIL_ICON = Identifier.fromNamespaceAndPath("asset_editor", "icons/pencil.svg");

    private final StudioContext context;
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
        nameLabel.setMaxWidth(160);
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

        context.packState().selectedPackProperty().addListener((obs, o, v) -> {
            refreshLabel();
            refreshPopoverContent();
        });
        context.packState().selectedNamespaceProperty().addListener((obs, o, v) -> {
            refreshLabel();
            refreshPopoverContent();
        });
        refreshLabel();

        setOnMouseEntered(e -> {
            if (!getStyleClass().contains("pack-selector-hover"))
                getStyleClass().add("pack-selector-hover");
        });
        setOnMouseExited(e -> getStyleClass().remove("pack-selector-hover"));
    }

    private void refreshLabel() {
        PackInfo pack = context.packState().selectedPack();
        if (pack == null) {
            nameLabel.setText(I18n.get("studio:pack.none"));
            nameLabel.setTextFill(VoxelColors.ZINC_500);
            return;
        }
        String ns = context.packState().selectedNamespace();
        nameLabel.setText(pack.name() + (ns != null ? "  ·  " + ns : ""));
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

        PackInfo selected = context.packState().selectedPack();

        if (selected != null) {
            popoverContent.getChildren().add(buildSelectedPackSection(selected));

            Region sep = new Region();
            sep.getStyleClass().add("pack-popover-separator");
            sep.setPrefHeight(1);
            sep.setMaxHeight(1);
            VBox.setMargin(sep, new Insets(4, 0, 4, 0));
            popoverContent.getChildren().add(sep);
        }

        for (PackInfo pack : context.packState().availablePacks()) {
            if (pack.equals(selected)) continue;
            popoverContent.getChildren().add(buildPackRow(pack));
        }

        Region separator = new Region();
        separator.getStyleClass().add("pack-popover-separator");
        separator.setPrefHeight(1);
        separator.setMaxHeight(1);
        VBox.setMargin(separator, new Insets(6, 0, 6, 0));

        popoverContent.getChildren().addAll(separator, buildCreateButton());
    }

    private VBox buildSelectedPackSection(PackInfo pack) {
        SvgIcon icon = new SvgIcon(FOLDER_ICON, 14, VoxelColors.ZINC_200);
        Label name = new Label(pack.name());
        name.setTextFill(VoxelColors.ZINC_100);
        name.setFont(VoxelFonts.of(VoxelFonts.Variant.SEMI_BOLD, 13));

        HBox titleRow = new HBox(10, icon, name);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        VBox section = new VBox(6, titleRow);
        section.getStyleClass().add("pack-popover-selected");
        section.setPadding(new Insets(10, 12, 10, 12));

        String activeNs = context.packState().selectedNamespace();

        VBox nsList = new VBox(2);
        nsList.setPadding(new Insets(4, 0, 0, 0));

        for (String ns : pack.namespaces()) {
            boolean isActive = ns.equals(activeNs);

            Label nsLabel = new Label(ns);
            nsLabel.setFont(VoxelFonts.of(VoxelFonts.Variant.MEDIUM, 11));
            nsLabel.setMaxWidth(Double.MAX_VALUE);
            nsLabel.setPadding(new Insets(4, 8, 4, 8));
            nsLabel.setCursor(Cursor.HAND);

            if (isActive) {
                nsLabel.getStyleClass().add("pack-popover-namespace-active");
                nsLabel.setTextFill(VoxelColors.ZINC_100);
            } else {
                nsLabel.getStyleClass().add("pack-popover-namespace");
                nsLabel.setTextFill(VoxelColors.ZINC_500);
                nsLabel.setOnMouseEntered(e -> nsLabel.getStyleClass().add("pack-popover-namespace-hover"));
                nsLabel.setOnMouseExited(e -> nsLabel.getStyleClass().remove("pack-popover-namespace-hover"));
            }

            nsLabel.setOnMouseClicked(e -> {
                e.consume();
                context.packState().selectNamespace(ns);
            });

            nsList.getChildren().add(nsLabel);
        }

        if (pack.namespaces().size() > 6) {
            javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(nsList);
            scroll.setFitToWidth(true);
            scroll.setMaxHeight(160);
            scroll.setPrefHeight(160);
            scroll.getStyleClass().add("pack-popover-ns-scroll");
            scroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
            section.getChildren().add(scroll);
        } else {
            section.getChildren().add(nsList);
        }
        return section;
    }

    private HBox buildPackRow(PackInfo pack) {
        SvgIcon icon = new SvgIcon(FOLDER_ICON, 14, VoxelColors.ZINC_400);

        Label name = new Label(pack.name());
        name.setTextFill(VoxelColors.ZINC_300);
        name.setFont(VoxelFonts.of(VoxelFonts.Variant.MEDIUM, 13));

        HBox row = new HBox(10, icon, name);
        row.getStyleClass().add("pack-popover-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 12, 8, 12));
        row.setCursor(Cursor.HAND);

        row.setOnMouseEntered(e -> {
            if (!row.getStyleClass().contains("pack-popover-row-hover"))
                row.getStyleClass().add("pack-popover-row-hover");
        });
        row.setOnMouseExited(e -> row.getStyleClass().remove("pack-popover-row-hover"));
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

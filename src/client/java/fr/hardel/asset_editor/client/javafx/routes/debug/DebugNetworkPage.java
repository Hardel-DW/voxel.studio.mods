package fr.hardel.asset_editor.client.javafx.routes.debug;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.components.ui.Button;
import fr.hardel.asset_editor.client.javafx.components.ui.DataTable;
import fr.hardel.asset_editor.client.javafx.lib.Page;
import fr.hardel.asset_editor.client.javafx.lib.debug.DebugLogStore;
import fr.hardel.asset_editor.client.javafx.lib.debug.DebugLogStore.Entry;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import net.minecraft.client.resources.language.I18n;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public final class DebugNetworkPage extends VBox implements Page {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault());
    private static final Color CLIENT_COLOR = Color.web("#34d399");
    private static final Color SERVER_COLOR = Color.web("#f87171");
    private static final Color MUTED_COLOR = VoxelColors.ZINC_500;

    private final DataTable<Entry> table = new DataTable<>();
    private final Label countLabel = new Label();
    private Runnable subscription;

    public DebugNetworkPage() {
        getStyleClass().add("debug-log-page");
        setFillWidth(true);

        table.addColumn(I18n.get("debug:network.column.time"), 100, this::timeCell);
        table.addColumn(I18n.get("debug:network.column.direction"), 80, this::directionCell);
        table.addColumn(I18n.get("debug:network.column.title"), -1, this::titleCell);
        table.setExpandFactory(this::detailNode);
        table.setIdExtractor(Entry::id);
        table.setPlaceholder(I18n.get("debug:network.placeholder"));
        VBox.setVgrow(table, Priority.ALWAYS);

        countLabel.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 12));
        countLabel.setTextFill(VoxelColors.ZINC_500);

        Button clearBtn = new Button(Button.Variant.GHOST_BORDER, Button.Size.SM, I18n.get("debug:action.clear"));
        clearBtn.setOnAction(() -> DebugLogStore.clearCategory(DebugLogStore.Category.NETWORK));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toolbar = new HBox(8, countLabel, spacer, clearBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(12, 16, 12, 16));

        getChildren().addAll(toolbar, table);

        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                if (subscription != null) {
                    subscription.run();
                    subscription = null;
                }
                return;
            }
            if (subscription == null)
                subscription = DebugLogStore.subscribe(() -> Platform.runLater(this::refresh));
            refresh();
        });

        refresh();
    }

    private void refresh() {
        var entries = DebugLogStore.entriesByCategory(DebugLogStore.Category.NETWORK);
        countLabel.setText(I18n.get("debug:network.count", entries.size()));
        table.setItems(entries);
    }

    private Label timeCell(Entry entry) {
        Label label = new Label(TIME_FORMAT.format(Instant.ofEpochMilli(entry.timestamp())));
        label.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 11));
        label.setTextFill(VoxelColors.ZINC_500);
        return label;
    }

    private Label titleCell(Entry entry) {
        Label label = new Label(entry.message());
        label.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 13));
        label.setTextFill(VoxelColors.ZINC_300);
        label.setWrapText(true);
        return label;
    }

    private Node detailNode(Entry entry) {
        if (entry.data() == null || entry.data().isEmpty()) {
            Label empty = new Label(I18n.get("debug:network.no_additional_data"));
            empty.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 12));
            empty.setTextFill(VoxelColors.ZINC_500);
            return empty;
        }

        String payloadId = entry.data().getOrDefault("payloadId", "");
        String description = entry.data().getOrDefault("payloadDescription", "");

        Label idLabel = new Label(payloadId);
        idLabel.setFont(VoxelFonts.of(VoxelFonts.Variant.SEMI_BOLD, 14));
        idLabel.setTextFill(VoxelColors.ZINC_100);
        idLabel.setWrapText(true);

        Label descriptionLabel = new Label(description);
        descriptionLabel.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 12));
        descriptionLabel.setTextFill(VoxelColors.ZINC_400);
        descriptionLabel.setWrapText(true);

        Region separator = new Region();
        separator.setPrefHeight(1);
        separator.setMaxWidth(Double.MAX_VALUE);
        separator.setStyle("-fx-background-color: rgba(63, 63, 70, 0.45);");

        VBox box = new VBox(8, idLabel, descriptionLabel, separator);
        for (Map.Entry<String, String> kv : entry.data().entrySet()) {
            if ("direction".equals(kv.getKey()) || "payloadId".equals(kv.getKey()) || "payloadDescription".equals(kv.getKey()))
                continue;

            Label key = new Label(kv.getKey());
            key.setFont(VoxelFonts.of(VoxelFonts.Variant.MEDIUM, 11));
            key.setTextFill(VoxelColors.ZINC_500);

            Label value = new Label(kv.getValue());
            value.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 12));
            value.setTextFill(VoxelColors.ZINC_300);
            value.setWrapText(true);

            VBox line = new VBox(2, key, value);
            line.setFillWidth(true);
            box.getChildren().add(line);
        }
        return box;
    }

    private TextFlow directionCellFlow(String direction) {
        String[] parts = direction.split("->");
        String left = parts.length > 0 ? parts[0].trim() : "?";
        String right = parts.length > 1 ? parts[1].trim() : "?";

        Text leftText = new Text(left);
        leftText.setFont(VoxelFonts.of(VoxelFonts.Variant.SEMI_BOLD, 11));
        leftText.setFill("C".equals(left) ? CLIENT_COLOR : SERVER_COLOR);

        Text arrowText = new Text(" -> ");
        arrowText.setFont(VoxelFonts.of(VoxelFonts.Variant.MEDIUM, 11));
        arrowText.setFill(MUTED_COLOR);

        Text rightText = new Text(right);
        rightText.setFont(VoxelFonts.of(VoxelFonts.Variant.SEMI_BOLD, 11));
        rightText.setFill("C".equals(right) ? CLIENT_COLOR : SERVER_COLOR);

        return new TextFlow(leftText, arrowText, rightText);
    }

    private Node directionCell(Entry entry) {
        String dir = entry.data().getOrDefault("direction", "?");
        return directionCellFlow(dir);
    }
}

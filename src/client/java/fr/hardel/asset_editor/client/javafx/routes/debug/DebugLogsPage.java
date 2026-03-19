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
import net.minecraft.client.resources.language.I18n;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public final class DebugLogsPage extends VBox implements Page {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    private static final Color INFO_COLOR = Color.web("#38bdf8");
    private static final Color WARN_COLOR = Color.web("#fbbf24");
    private static final Color ERROR_COLOR = Color.web("#f87171");
    private static final Color SUCCESS_COLOR = Color.web("#34d399");

    private final DataTable<Entry> table = new DataTable<>();
    private final Label countLabel = new Label();
    private Runnable subscription;

    public DebugLogsPage() {
        getStyleClass().add("debug-log-page");
        setFillWidth(true);

        table.addColumn(I18n.get("debug:logs.column.time"), 100, this::timeCell);
        table.addColumn(I18n.get("debug:logs.column.level"), 80, this::levelCell);
        table.addColumn(I18n.get("debug:logs.column.category"), 100, this::categoryCell);
        table.addColumn(I18n.get("debug:logs.column.message"), -1, this::messageCell);
        table.setExpandFactory(this::detailNode);
        table.setIdExtractor(Entry::id);
        table.setPlaceholder(I18n.get("debug:logs.placeholder"));
        VBox.setVgrow(table, Priority.ALWAYS);

        countLabel.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 12));
        countLabel.setTextFill(VoxelColors.ZINC_500);

        Button clearBtn = new Button(Button.Variant.GHOST_BORDER, Button.Size.SM, I18n.get("debug:action.clear"));
        clearBtn.setOnAction(DebugLogStore::clear);

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
        var entries = DebugLogStore.entries().stream()
            .filter(entry -> entry.category() != DebugLogStore.Category.NETWORK)
            .toList();
        countLabel.setText(I18n.get("debug:logs.count", entries.size()));
        table.setItems(entries);
    }

    private Label timeCell(Entry entry) {
        Label label = new Label(TIME_FORMAT.format(Instant.ofEpochMilli(entry.timestamp())));
        label.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 11));
        label.setTextFill(VoxelColors.ZINC_500);
        return label;
    }

    private Label levelCell(Entry entry) {
        Label label = new Label(I18n.get("debug:logs.level." + entry.level().name().toLowerCase()));
        label.setFont(VoxelFonts.of(VoxelFonts.Variant.SEMI_BOLD, 11));
        label.setTextFill(levelColor(entry.level()));
        return label;
    }

    private Label categoryCell(Entry entry) {
        Label label = new Label(I18n.get("debug:logs.category." + entry.category().name().toLowerCase()));
        label.setFont(VoxelFonts.of(VoxelFonts.Variant.MEDIUM, 11));
        label.setTextFill(VoxelColors.ZINC_400);
        return label;
    }

    private Label messageCell(Entry entry) {
        Label label = new Label(entry.message());
        label.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 13));
        label.setTextFill(VoxelColors.ZINC_300);
        label.setWrapText(true);
        return label;
    }

    private Node detailNode(Entry entry) {
        if (entry.data() == null || entry.data().isEmpty()) {
            Label empty = new Label(I18n.get("debug:logs.no_additional_data"));
            empty.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 12));
            empty.setTextFill(VoxelColors.ZINC_500);
            return empty;
        }

        VBox box = new VBox(4);
        for (Map.Entry<String, String> kv : entry.data().entrySet()) {
            Label line = new Label(kv.getKey() + ": " + kv.getValue());
            line.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 12));
            line.setTextFill(VoxelColors.ZINC_400);
            line.setWrapText(true);
            box.getChildren().add(line);
        }
        return box;
    }

    private static Color levelColor(DebugLogStore.Level level) {
        return switch (level) {
            case INFO -> INFO_COLOR;
            case WARN -> WARN_COLOR;
            case ERROR -> ERROR_COLOR;
            case SUCCESS -> SUCCESS_COLOR;
        };
    }
}

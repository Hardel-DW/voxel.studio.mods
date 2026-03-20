package fr.hardel.asset_editor.client.javafx.routes.debug;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.client.debug.NetworkTraceStore;
import fr.hardel.asset_editor.client.debug.NetworkTraceStore.TraceEntry;
import fr.hardel.asset_editor.client.debug.RecordIntrospector;
import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.components.ui.Button;
import fr.hardel.asset_editor.client.javafx.components.ui.CopyButton;
import fr.hardel.asset_editor.client.javafx.components.ui.DataTable;
import fr.hardel.asset_editor.client.javafx.components.ui.Dropdown;
import fr.hardel.asset_editor.client.javafx.components.ui.KeyValueGrid;
import fr.hardel.asset_editor.client.javafx.lib.Page;
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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class DebugNetworkPage extends VBox implements Page {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    private final DataTable<TraceEntry> table = new DataTable<>();
    private final Label countLabel = new Label();
    private final Dropdown<String> namespaceDd;
    private Runnable subscription;

    public DebugNetworkPage() {
        getStyleClass().add("debug-log-page");
        setFillWidth(true);

        table.addColumn(I18n.get("debug:network.column.time"), 100, this::timeCell);
        table.addColumn(I18n.get("debug:network.column.direction"), 80, this::directionCell);
        table.addColumn(I18n.get("debug:network.column.payload_id"), 220, this::payloadIdCell);
        table.addColumn(I18n.get("debug:network.column.title"), -1, this::titleCell);
        table.addColumn("", 40, this::copyCell);
        table.setExpandFactory(this::detailNode);
        table.setIdExtractor(TraceEntry::id);
        table.setPlaceholder(I18n.get("debug:network.placeholder"));
        table.setPagination(50);
        VBox.setVgrow(table, Priority.ALWAYS);

        countLabel.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 12));
        countLabel.setTextFill(VoxelColors.ZINC_500);

        String allLabel = I18n.get("generic:all");
        List<String> namespaces = new ArrayList<>();
        namespaces.add(allLabel);
        namespaces.addAll(NetworkTraceStore.availableNamespaces());

        namespaceDd = new Dropdown<>(namespaces, AssetEditor.MOD_ID, Function.identity(), ns -> {
            if (ns.equals(allLabel))
                NetworkTraceStore.setFilter(id -> true);
            else
                NetworkTraceStore.setFilter(id -> ns.equals(id.getNamespace()));
        });

        Button clearBtn = new Button(Button.Variant.GHOST_BORDER, Button.Size.SM, I18n.get("debug:action.clear"));
        clearBtn.setOnAction(NetworkTraceStore::clear);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toolbar = new HBox(8, countLabel, namespaceDd, spacer, clearBtn);
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
                subscription = NetworkTraceStore.subscribe(() -> Platform.runLater(this::refresh));
            refresh();
        });

        refresh();
    }

    private void refresh() {
        var entries = NetworkTraceStore.entries();
        countLabel.setText(I18n.get("debug:network.count", entries.size()));

        List<String> namespaces = new ArrayList<>();
        namespaces.add(I18n.get("generic:all"));
        namespaces.addAll(NetworkTraceStore.availableNamespaces());
        namespaceDd.setItems(namespaces);

        table.setItems(entries);
    }

    private Label timeCell(TraceEntry entry) {
        Label label = new Label(TIME_FORMAT.format(Instant.ofEpochMilli(entry.timestamp())));
        label.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 11));
        label.setTextFill(VoxelColors.ZINC_500);
        return label;
    }

    private Node directionCell(TraceEntry entry) {
        boolean inbound = entry.direction() == NetworkTraceStore.Direction.INBOUND;
        String left = inbound ? "S" : "C";
        String right = inbound ? "C" : "S";
        Color leftColor = inbound ? VoxelColors.RED_400 : VoxelColors.EMERALD_400;
        Color rightColor = inbound ? VoxelColors.EMERALD_400 : VoxelColors.RED_400;

        Label leftLabel = new Label(left);
        leftLabel.setFont(VoxelFonts.of(VoxelFonts.Variant.SEMI_BOLD, 11));
        leftLabel.setTextFill(leftColor);

        Label arrow = new Label(" \u2192 ");
        arrow.setFont(VoxelFonts.of(VoxelFonts.Variant.MEDIUM, 11));
        arrow.setTextFill(VoxelColors.ZINC_500);

        Label rightLabel = new Label(right);
        rightLabel.setFont(VoxelFonts.of(VoxelFonts.Variant.SEMI_BOLD, 11));
        rightLabel.setTextFill(rightColor);

        HBox box = new HBox(leftLabel, arrow, rightLabel);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private Label payloadIdCell(TraceEntry entry) {
        Label label = new Label(entry.payloadId().getPath());
        label.setFont(VoxelFonts.of(VoxelFonts.Variant.MEDIUM, 11));
        label.setTextFill(VoxelColors.ZINC_400);
        return label;
    }

    private Node titleCell(TraceEntry entry) {
        String base = "debug:payload." + entry.payloadId().getNamespace() + "." + entry.payloadId().getPath().replace('/', '.');
        String titleKey = base + ".title";
        String descKey = base + ".description";
        String title = I18n.exists(titleKey) ? I18n.get(titleKey) : entry.payloadId().getPath();

        Label titleLabel = new Label(title);
        titleLabel.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 13));
        titleLabel.setTextFill(VoxelColors.ZINC_300);
        titleLabel.setWrapText(true);

        if (I18n.exists(descKey)) {
            Label descLabel = new Label(I18n.get(descKey));
            descLabel.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 11));
            descLabel.setTextFill(VoxelColors.ZINC_500);
            descLabel.setWrapText(true);
            VBox box = new VBox(2, titleLabel, descLabel);
            box.setMaxWidth(Double.MAX_VALUE);
            return box;
        }

        return titleLabel;
    }

    private Node copyCell(TraceEntry entry) {
        return new CopyButton(() -> serializeEntry(entry));
    }

    private Node detailNode(TraceEntry entry) {
        Object payload = entry.payload();
        if (payload == null || !payload.getClass().isRecord())
            return emptyDetail();

        return new KeyValueGrid(payload);
    }

    private static String serializeEntry(TraceEntry entry) {
        JsonObject json = new JsonObject();
        json.addProperty("id", entry.id());
        json.addProperty("timestamp", entry.timestamp());
        json.addProperty("direction", entry.direction().name());
        json.addProperty("payloadId", entry.payloadId().toString());
        if (entry.payload() != null && entry.payload().getClass().isRecord())
            json.add("payload", fieldsToJson(RecordIntrospector.introspect(entry.payload())));
        return new GsonBuilder().setPrettyPrinting().create().toJson(json);
    }

    private static JsonElement fieldsToJson(List<RecordIntrospector.Field> fields) {
        JsonObject obj = new JsonObject();
        for (var field : fields)
            obj.add(field.name(), fieldValueToJson(field.value()));
        return obj;
    }

    private static JsonElement fieldValueToJson(RecordIntrospector.FieldValue value) {
        return switch (value) {
            case RecordIntrospector.FieldValue.Scalar s -> new JsonPrimitive(s.text());
            case RecordIntrospector.FieldValue.Nested n -> fieldsToJson(n.children());
            case RecordIntrospector.FieldValue.Items items -> {
                JsonArray arr = new JsonArray();
                for (var item : items.preview())
                    arr.add(fieldValueToJson(item));
                yield arr;
            }
        };
    }

    private static Label emptyDetail() {
        Label label = new Label(I18n.get("debug:network.no_additional_data"));
        label.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 12));
        label.setTextFill(VoxelColors.ZINC_500);
        return label;
    }
}

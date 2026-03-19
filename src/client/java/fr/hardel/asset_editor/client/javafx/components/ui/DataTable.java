package fr.hardel.asset_editor.client.javafx.components.ui;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public final class DataTable<T> extends VBox {

    public record Column<T>(String header, double width, Function<T, Node> cellFactory) {}

    private final List<Column<T>> columns = new ArrayList<>();
    private final VBox body = new VBox();
    private final HBox headerRow = new HBox();
    private final Label placeholder = new Label();
    private final Set<Long> expandedIds = new HashSet<>();
    private Function<T, Node> expandFactory;
    private Function<T, Long> idExtractor;
    private List<T> items = List.of();

    public DataTable() {
        getStyleClass().add("ui-data-table");
        setFillWidth(true);

        headerRow.getStyleClass().add("ui-data-table-header");
        headerRow.setPadding(new Insets(0, 16, 0, 16));
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setMinHeight(40);
        headerRow.setMaxHeight(40);

        body.getStyleClass().add("ui-data-table-body");
        body.setFillWidth(true);

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("ui-data-table-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        placeholder.setFont(VoxelFonts.of(VoxelFonts.Variant.MEDIUM, 14));
        placeholder.setTextFill(VoxelColors.ZINC_500);
        placeholder.setAlignment(Pos.CENTER);
        placeholder.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        VBox.setVgrow(placeholder, Priority.ALWAYS);
        placeholder.setVisible(false);
        placeholder.setManaged(false);

        getChildren().addAll(headerRow, scroll, placeholder);
    }

    public void addColumn(String header, double width, Function<T, Node> cellFactory) {
        columns.add(new Column<>(header, width, cellFactory));
        rebuildHeader();
    }

    public void setExpandFactory(Function<T, Node> factory) {
        this.expandFactory = factory;
    }

    public void setIdExtractor(Function<T, Long> extractor) {
        this.idExtractor = extractor;
    }

    public void setPlaceholder(String text) {
        placeholder.setText(text);
    }

    public void setItems(List<T> items) {
        this.items = items == null ? List.of() : items;
        rebuild();
    }

    private void rebuildHeader() {
        headerRow.getChildren().clear();
        for (Column<T> col : columns) {
            Label label = new Label(col.header().toUpperCase());
            label.setFont(VoxelFonts.of(VoxelFonts.Variant.MEDIUM, 11));
            label.setTextFill(VoxelColors.ZINC_500);
            applyColumnWidth(label, col.width());
            headerRow.getChildren().add(label);
        }
    }

    private void rebuild() {
        body.getChildren().clear();
        boolean empty = items.isEmpty();
        placeholder.setVisible(empty);
        placeholder.setManaged(empty);

        for (int i = 0; i < items.size(); i++) {
            T item = items.get(i);
            long rowId = idExtractor != null ? idExtractor.apply(item) : i;
            boolean expanded = expandedIds.contains(rowId);

            HBox row = buildRow(item, rowId);
            if (i % 2 == 1)
                row.getStyleClass().add("ui-data-table-row-alt");

            VBox wrapper = new VBox(row);
            wrapper.setFillWidth(true);
            if (expanded && expandFactory != null) {
                Node detail = expandFactory.apply(item);
                VBox detailBox = new VBox(detail);
                detailBox.getStyleClass().add("ui-data-table-detail");
                detailBox.setPadding(new Insets(8, 16, 12, 16));
                wrapper.getChildren().add(detailBox);
            }
            body.getChildren().add(wrapper);
        }
    }

    private HBox buildRow(T item, long rowId) {
        HBox row = new HBox();
        row.getStyleClass().add("ui-data-table-row");
        row.setPadding(new Insets(0, 16, 0, 16));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMinHeight(36);
        row.setMaxWidth(Double.MAX_VALUE);

        for (Column<T> col : columns) {
            Node cell = col.cellFactory().apply(item);
            applyColumnWidth(cell, col.width());
            row.getChildren().add(cell);
        }

        if (expandFactory != null) {
            row.setCursor(Cursor.HAND);
            row.setOnMouseClicked(e -> {
                if (expandedIds.contains(rowId))
                    expandedIds.remove(rowId);
                else
                    expandedIds.add(rowId);
                rebuild();
            });
        }

        return row;
    }

    private static void applyColumnWidth(Node node, double width) {
        if (node instanceof Region region) {
            if (width < 0) {
                HBox.setHgrow(region, Priority.ALWAYS);
                region.setMaxWidth(Double.MAX_VALUE);
            } else {
                region.setMinWidth(width);
                region.setPrefWidth(width);
                region.setMaxWidth(width);
            }
        }
    }
}

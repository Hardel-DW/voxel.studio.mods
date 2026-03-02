package fr.hardel.asset_editor.client.javafx.components.ui;

import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.List;

public final class AutoFitGrid extends GridPane {

    private static final double XL_BREAKPOINT = 1280.0;

    private final double minColumnWidth;
    private final boolean singleColumnAtAndBelowXl;

    public AutoFitGrid(double minColumnWidth, boolean singleColumnAtAndBelowXl) {
        this.minColumnWidth = minColumnWidth;
        this.singleColumnAtAndBelowXl = singleColumnAtAndBelowXl;
        setHgap(16);
        setVgap(16);
        setMaxWidth(Double.MAX_VALUE);
        widthProperty().addListener((obs, oldValue, newValue) -> refreshLayout());
        getChildren().addListener((ListChangeListener<Node>) change -> refreshLayout());
    }

    public void addItem(Node node) {
        getChildren().add(node);
    }

    private void refreshLayout() {
        int itemCount = getChildren().size();
        if (itemCount == 0) {
            getColumnConstraints().clear();
            return;
        }

        int columns = computeColumnCount(itemCount);
        getColumnConstraints().setAll(createColumns(columns));

        int col = 0;
        int row = 0;
        for (Node child : getChildren()) {
            if (child instanceof Region region) {
                region.setMaxWidth(Double.MAX_VALUE);
            }
            setColumnIndex(child, col);
            setRowIndex(child, row);
            setHgrow(child, Priority.ALWAYS);
            setFillWidth(child, true);
            setVgrow(child, Priority.ALWAYS);
            setFillHeight(child, true);

            col++;
            if (col >= columns) {
                col = 0;
                row++;
            }
        }
    }

    private int computeColumnCount(int itemCount) {
        double width = Math.max(getWidth(), getLayoutBounds().getWidth());
        if (width <= 0) {
            return 1;
        }
        if (singleColumnAtAndBelowXl && width <= XL_BREAKPOINT) {
            return 1;
        }

        int maxColumnsThatFit = (int) Math.floor((width + getHgap()) / (minColumnWidth + getHgap()));
        int normalizedColumns = Math.max(1, maxColumnsThatFit);
        return Math.min(itemCount, normalizedColumns);
    }

    private List<ColumnConstraints> createColumns(int columns) {
        double percent = 100.0 / columns;
        ArrayList<ColumnConstraints> constraints = new ArrayList<>(columns);
        for (int i = 0; i < columns; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(percent);
            col.setFillWidth(true);
            col.setHgrow(Priority.ALWAYS);
            constraints.add(col);
        }
        return constraints;
    }
}

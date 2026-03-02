package fr.hardel.asset_editor.client.javafx.components.ui;

import fr.hardel.asset_editor.client.javafx.lib.data.StudioBreakpoint;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.List;

public final class ResponsiveGrid extends GridPane {

    private final List<Rule> rules = new ArrayList<>();
    private final LayoutSpec defaultSpec;

    public ResponsiveGrid(LayoutSpec defaultSpec) {
        this.defaultSpec = defaultSpec;
        setHgap(16);
        setVgap(16);
        setMaxWidth(Double.MAX_VALUE);
        widthProperty().addListener((obs, oldValue, newValue) -> refreshLayout());
        getChildren().addListener((ListChangeListener<Node>) change -> refreshLayout());
    }

    public static LayoutSpec autoFit(double minColumnWidth) {
        return autoFit(minColumnWidth, Integer.MAX_VALUE);
    }

    public static LayoutSpec autoFit(double minColumnWidth, int maxColumns) {
        return new AutoFitSpec(Math.max(1, minColumnWidth), Math.max(1, maxColumns));
    }

    public static LayoutSpec fixed(double... frWeights) {
        if (frWeights == null || frWeights.length == 0) {
            throw new IllegalArgumentException("fixed() requires at least one column weight");
        }
        double[] normalized = frWeights.clone();
        for (int i = 0; i < normalized.length; i++) {
            normalized[i] = Math.max(0.0001, normalized[i]);
        }
        return new FixedSpec(normalized);
    }

    public ResponsiveGrid atMost(StudioBreakpoint breakpoint, LayoutSpec spec) {
        return atMost(breakpoint.px(), spec);
    }

    public ResponsiveGrid atLeast(StudioBreakpoint breakpoint, LayoutSpec spec) {
        return atLeast(breakpoint.px(), spec);
    }

    public ResponsiveGrid atMost(double maxWidth, LayoutSpec spec) {
        rules.add(new Rule(null, maxWidth, spec));
        refreshLayout();
        return this;
    }

    public ResponsiveGrid atLeast(double minWidth, LayoutSpec spec) {
        rules.add(new Rule(minWidth, null, spec));
        refreshLayout();
        return this;
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

        double width = contentWidth();
        LayoutSpec activeSpec = resolveSpec(width);
        int columns = computeColumns(activeSpec, itemCount, width);
        getColumnConstraints().setAll(createColumns(activeSpec, columns));

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

    private double contentWidth() {
        double width = Math.max(getWidth(), getLayoutBounds().getWidth());
        var insets = getInsets();
        if (insets != null) {
            width -= insets.getLeft() + insets.getRight();
        }
        return width;
    }

    private LayoutSpec resolveSpec(double width) {
        LayoutSpec active = defaultSpec;
        for (Rule rule : rules) {
            if (rule.matches(width)) {
                active = rule.spec();
            }
        }
        return active;
    }

    private int computeColumns(LayoutSpec spec, int itemCount, double width) {
        if (spec instanceof FixedSpec fixedSpec) {
            return Math.min(itemCount, fixedSpec.columnCount());
        }
        if (width <= 0) {
            return 1;
        }

        AutoFitSpec autoFitSpec = (AutoFitSpec) spec;
        int fit = (int) Math.floor((width + getHgap()) / (autoFitSpec.minColumnWidth() + getHgap()));
        int normalized = Math.max(1, fit);
        return Math.min(itemCount, Math.min(autoFitSpec.maxColumns(), normalized));
    }

    private List<ColumnConstraints> createColumns(LayoutSpec spec, int columns) {
        ArrayList<ColumnConstraints> constraints = new ArrayList<>(columns);
        if (spec instanceof FixedSpec fixedSpec) {
            double total = fixedSpec.totalWeight();
            for (int i = 0; i < columns; i++) {
                double weight = fixedSpec.weightAt(i);
                ColumnConstraints col = new ColumnConstraints();
                col.setPercentWidth((weight / total) * 100.0);
                col.setFillWidth(true);
                col.setHgrow(Priority.ALWAYS);
                constraints.add(col);
            }
            return constraints;
        }

        double percent = 100.0 / columns;
        for (int i = 0; i < columns; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(percent);
            col.setFillWidth(true);
            col.setHgrow(Priority.ALWAYS);
            constraints.add(col);
        }
        return constraints;
    }

    public sealed interface LayoutSpec permits AutoFitSpec, FixedSpec {
    }

    public record AutoFitSpec(double minColumnWidth, int maxColumns) implements LayoutSpec {
    }

    public static final class FixedSpec implements LayoutSpec {
        private final double[] frWeights;
        private final double totalWeight;

        private FixedSpec(double[] frWeights) {
            this.frWeights = frWeights;
            double total = 0.0;
            for (double weight : frWeights) {
                total += weight;
            }
            this.totalWeight = total;
        }

        private int columnCount() {
            return frWeights.length;
        }

        private double totalWeight() {
            return totalWeight;
        }

        private double weightAt(int index) {
            return frWeights[Math.min(index, frWeights.length - 1)];
        }
    }

    private record Rule(Double minWidth, Double maxWidth, LayoutSpec spec) {
        private boolean matches(double width) {
            boolean minOk = minWidth == null || width >= minWidth;
            boolean maxOk = maxWidth == null || width <= maxWidth;
            return minOk && maxOk;
        }
    }
}

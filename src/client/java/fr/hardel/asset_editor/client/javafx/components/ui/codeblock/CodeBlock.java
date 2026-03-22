package fr.hardel.asset_editor.client.javafx.components.ui.codeblock;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.highlight.Highlight;
import fr.hardel.asset_editor.client.highlight.HighlightPalette;
import fr.hardel.asset_editor.client.highlight.HighlightRange;
import fr.hardel.asset_editor.client.highlight.HighlightRegistry;
import fr.hardel.asset_editor.client.highlight.HighlightStyle;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.HLineTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.shape.VLineTo;
import javafx.scene.text.Font;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CodeBlock extends Region {

    private final ScrollPane scrollPane = new ScrollPane();
    private final Pane contentPane = new Pane();
    private final Text baseText = new Text();
    private final TextFlow visibleTextFlow = new TextFlow();
    private final HighlightRegistry highlights = new HighlightRegistry();
    private final HighlightPalette palette = new HighlightPalette();
    private final StringProperty text = new SimpleStringProperty(this, "text", "");
    private final ObjectProperty<CodeBlockHighlighter> highlighter = new SimpleObjectProperty<>(this, "highlighter");
    private final ObjectProperty<Font> font = new SimpleObjectProperty<>(this, "font", Font.font("Monospaced", 13));
    private final ObjectProperty<FontSmoothingType> fontSmoothingType = new SimpleObjectProperty<>(this, "fontSmoothingType", FontSmoothingType.LCD);
    private final ObjectProperty<Paint> textFill = new SimpleObjectProperty<>(this, "textFill", Color.web("#abb2bf"));
    private final ObjectProperty<Paint> backgroundFill = new SimpleObjectProperty<>(this, "backgroundFill", VoxelColors.ZINC_950);
    private final ObjectProperty<Paint> borderFill = new SimpleObjectProperty<>(this, "borderFill", VoxelColors.ZINC_800);
    private final ObjectProperty<Insets> contentPadding = new SimpleObjectProperty<>(this, "contentPadding", new Insets(14));
    private final DoubleProperty lineSpacing = new SimpleDoubleProperty(this, "lineSpacing", 4);
    private final BooleanProperty wrapText = new SimpleBooleanProperty(this, "wrapText", false);
    private final List<Node> dynamicNodes = new ArrayList<>();

    private boolean geometryDirty = true;
    private boolean renderDirty = true;

    public CodeBlock() {
        getChildren().add(scrollPane);
        setFocusTraversable(false);
        scrollPane.setContent(contentPane);
        scrollPane.setFitToHeight(true);
        scrollPane.setPannable(true);
        scrollPane.setBackground(Background.EMPTY);
        scrollPane.setFocusTraversable(false);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-padding: 0;");
        contentPane.setBackground(Background.EMPTY);
        contentPane.setFocusTraversable(false);

        baseText.setManaged(false);
        baseText.setMouseTransparent(true);
        baseText.setTextOrigin(VPos.TOP);
        baseText.setBoundsType(TextBoundsType.LOGICAL);
        baseText.setOpacity(0);

        visibleTextFlow.setManaged(false);
        visibleTextFlow.setMouseTransparent(true);
        visibleTextFlow.setFocusTraversable(false);

        contentPane.getChildren().addAll(baseText, visibleTextFlow);

        text.addListener((obs, oldValue, newValue) -> {
            baseText.setText(newValue == null ? "" : newValue);
            if (getHighlighter() != null)
                refreshHighlights();
            markGeometryDirty();
        });

        highlighter.addListener((obs, oldValue, newValue) -> {
            if (newValue == null) {
                highlights.clear();
                markRenderDirty();
            } else {
                refreshHighlights();
            }
        });

        font.addListener((obs, oldValue, newValue) -> markGeometryDirty());
        fontSmoothingType.addListener((obs, oldValue, newValue) -> markRenderDirty());
        textFill.addListener((obs, oldValue, newValue) -> {
            baseText.setFill(newValue);
            markRenderDirty();
        });
        lineSpacing.addListener((obs, oldValue, newValue) -> markGeometryDirty());
        contentPadding.addListener((obs, oldValue, newValue) -> markGeometryDirty());
        wrapText.addListener((obs, oldValue, newValue) -> markGeometryDirty());
        backgroundFill.addListener((obs, oldValue, newValue) -> applyContainerStyle());
        borderFill.addListener((obs, oldValue, newValue) -> applyContainerStyle());
        scrollPane.viewportBoundsProperty().addListener((obs, oldValue, newValue) -> {
            if (isWrapText() && (oldValue == null || oldValue.getWidth() != newValue.getWidth()))
                markGeometryDirty();
        });

        widthProperty().addListener((obs, oldValue, newValue) -> {
            if (isWrapText())
                markGeometryDirty();
        });
        scrollPane.skinProperty().addListener((obs, oldValue, newValue) -> updateScrollPaneViewportStyle());
        scrollPane.sceneProperty().addListener((obs, oldValue, newValue) -> updateScrollPaneViewportStyle());

        highlights.addListener(this::markRenderDirty);
        palette.addListener(this::markRenderDirty);

        baseText.setText(getText());
        applyContainerStyle();
        updateScrollPaneViewportStyle();
        refreshHighlights();
    }

    public HighlightRegistry highlights() {
        return highlights;
    }

    public HighlightPalette palette() {
        return palette;
    }

    public void refreshHighlights() {
        highlights.clear();
        CodeBlockHighlighter value = getHighlighter();
        if (value != null)
            value.apply(getText(), highlights);

        markRenderDirty();
    }

    public String getText() {
        return text.get();
    }

    public void setText(String value) {
        text.set(value);
    }

    public StringProperty textProperty() {
        return text;
    }

    public CodeBlockHighlighter getHighlighter() {
        return highlighter.get();
    }

    public void setHighlighter(CodeBlockHighlighter value) {
        highlighter.set(value);
    }

    public ObjectProperty<CodeBlockHighlighter> highlighterProperty() {
        return highlighter;
    }

    public Font getFont() {
        return font.get();
    }

    public void setFont(Font value) {
        font.set(value);
    }

    public ObjectProperty<Font> fontProperty() {
        return font;
    }

    public Paint getTextFill() {
        return textFill.get();
    }

    public void setTextFill(Paint value) {
        textFill.set(value);
    }

    public ObjectProperty<Paint> textFillProperty() {
        return textFill;
    }

    public FontSmoothingType getFontSmoothingType() {
        return fontSmoothingType.get();
    }

    public void setFontSmoothingType(FontSmoothingType value) {
        fontSmoothingType.set(value);
    }

    public ObjectProperty<FontSmoothingType> fontSmoothingTypeProperty() {
        return fontSmoothingType;
    }

    public Paint getBackgroundFill() {
        return backgroundFill.get();
    }

    public void setBackgroundFill(Paint value) {
        backgroundFill.set(value);
    }

    public ObjectProperty<Paint> backgroundFillProperty() {
        return backgroundFill;
    }

    public Paint getBorderFill() {
        return borderFill.get();
    }

    public void setBorderFill(Paint value) {
        borderFill.set(value);
    }

    public ObjectProperty<Paint> borderFillProperty() {
        return borderFill;
    }

    public Insets getContentPadding() {
        return contentPadding.get();
    }

    public void setContentPadding(Insets value) {
        contentPadding.set(value);
    }

    public ObjectProperty<Insets> contentPaddingProperty() {
        return contentPadding;
    }

    public double getLineSpacing() {
        return lineSpacing.get();
    }

    public void setLineSpacing(double value) {
        lineSpacing.set(value);
    }

    public DoubleProperty lineSpacingProperty() {
        return lineSpacing;
    }

    public boolean isWrapText() {
        return wrapText.get();
    }

    public void setWrapText(boolean value) {
        wrapText.set(value);
    }

    public BooleanProperty wrapTextProperty() {
        return wrapText;
    }

    @Override
    protected void layoutChildren() {
        double inset = 1;
        double width = Math.max(0, getWidth() - inset * 2);
        double height = Math.max(0, getHeight() - inset * 2);
        scrollPane.resizeRelocate(inset, inset, width, height);
        updateBaseTextGeometry();
        if (renderDirty)
            rebuildRender();
    }

    @Override
    protected double computePrefWidth(double height) {
        Insets padding = getContentPadding();
        return snapSizeX(baseText.prefWidth(-1) + padding.getLeft() + padding.getRight());
    }

    @Override
    protected double computePrefHeight(double width) {
        Insets padding = getContentPadding();
        return snapSizeY(baseText.prefHeight(width) + padding.getTop() + padding.getBottom());
    }

    private void markGeometryDirty() {
        geometryDirty = true;
        renderDirty = true;
        requestLayout();
    }

    private void markRenderDirty() {
        renderDirty = true;
        requestLayout();
    }

    private void applyContainerStyle() {
        BackgroundFill fill = new BackgroundFill(getBackgroundFill(), new CornerRadii(10), Insets.EMPTY);
        setBackground(new Background(fill));
        setBorder(new Border(new BorderStroke(getBorderFill(), BorderStrokeStyle.SOLID, new CornerRadii(10), BorderStroke.THIN)));
        contentPane.setBackground(new Background(fill));
    }

    private void updateBaseTextGeometry() {
        if (!geometryDirty)
            return;

        Insets padding = getContentPadding();
        double viewportWidth = scrollPane.getViewportBounds().getWidth();
        double wrappingWidth = isWrapText() ? Math.max(0, viewportWidth - padding.getLeft() - padding.getRight()) : 0;

        configureTextNode(baseText, getTextFill(), wrappingWidth);
        baseText.setX(padding.getLeft());
        baseText.setY(padding.getTop());
        visibleTextFlow.setLayoutX(padding.getLeft());
        visibleTextFlow.setLayoutY(padding.getTop());
        visibleTextFlow.setLineSpacing(getLineSpacing());
        visibleTextFlow.setPrefWidth(isWrapText() ? wrappingWidth : Region.USE_COMPUTED_SIZE);

        double contentWidth = baseText.getLayoutBounds().getWidth() + padding.getLeft() + padding.getRight();
        double contentHeight = baseText.getLayoutBounds().getHeight() + padding.getTop() + padding.getBottom();
        contentPane.setMinSize(contentWidth, contentHeight);
        contentPane.setPrefSize(contentWidth, contentHeight);

        geometryDirty = false;
    }

    private void rebuildRender() {
        renderDirty = false;
        clearDynamicNodes();

        List<Node> backgrounds = new ArrayList<>();
        List<Node> underlines = new ArrayList<>();

        for (HighlightRegistry.Entry entry : highlights.entriesInPaintOrder()) {
            HighlightStyle style = palette.get(entry.name());
            if (style == null)
                continue;

            Highlight highlight = entry.highlight();

            if (style.hasBackground()) {
                Path backgroundShape = buildRangePath(highlight, false);
                if (backgroundShape != null) {
                    backgroundShape.setFill(style.background());
                    backgrounds.add(backgroundShape);
                }
            }

            if (style.hasUnderline()) {
                Path underlineShape = buildRangePath(highlight, true);
                if (underlineShape != null) {
                    underlineShape.setFill(style.underline());
                    underlines.add(underlineShape);
                }
            }
        }

        rebuildVisibleTextRuns();

        dynamicNodes.addAll(backgrounds);
        dynamicNodes.addAll(underlines);

        contentPane.getChildren().setAll(backgrounds);
        contentPane.getChildren().add(baseText);
        contentPane.getChildren().add(visibleTextFlow);
        contentPane.getChildren().addAll(underlines);
    }

    private void clearDynamicNodes() {
        contentPane.getChildren().removeAll(dynamicNodes);
        dynamicNodes.clear();
    }

    private void configureTextNode(Text node, Paint fill, double wrappingWidth) {
        node.setManaged(false);
        node.setMouseTransparent(true);
        node.setTextOrigin(VPos.TOP);
        node.setBoundsType(TextBoundsType.LOGICAL);
        node.setText(getText());
        node.setFont(getFont());
        node.setFontSmoothingType(getFontSmoothingType());
        node.setFill(fill);
        node.setLineSpacing(getLineSpacing());
        node.setWrappingWidth(wrappingWidth);
    }

    private Path buildRangePath(Highlight highlight, boolean underline) {
        int textLength = getText().length();
        Path path = new Path();
        path.setManaged(false);
        path.setMouseTransparent(true);

        boolean hasElements = false;
        for (HighlightRange range : highlight.ranges()) {
            HighlightRange clamped = range.clampToLength(textLength);
            if (clamped.isCollapsed())
                continue;

            PathElement[] source = underline
                ? baseText.underlineShape(clamped.start(), clamped.end())
                : baseText.rangeShape(clamped.start(), clamped.end());

            if (source == null || source.length == 0)
                continue;

            hasElements = true;
            for (PathElement element : source)
                path.getElements().add(copyPathElement(element));
        }

        return hasElements ? path : null;
    }

    private PathElement copyPathElement(PathElement element) {
        if (element instanceof MoveTo moveTo) {
            return new MoveTo(moveTo.getX(), moveTo.getY());
        }
        if (element instanceof LineTo lineTo) {
            return new LineTo(lineTo.getX(), lineTo.getY());
        }
        if (element instanceof HLineTo hLineTo) {
            return new HLineTo(hLineTo.getX());
        }
        if (element instanceof VLineTo vLineTo) {
            return new VLineTo(vLineTo.getY());
        }
        if (element instanceof CubicCurveTo cubicCurveTo) {
            return new CubicCurveTo(
                cubicCurveTo.getControlX1(),
                cubicCurveTo.getControlY1(),
                cubicCurveTo.getControlX2(),
                cubicCurveTo.getControlY2(),
                cubicCurveTo.getX(),
                cubicCurveTo.getY());
        }
        if (element instanceof QuadCurveTo quadCurveTo) {
            return new QuadCurveTo(
                quadCurveTo.getControlX(),
                quadCurveTo.getControlY(),
                quadCurveTo.getX(),
                quadCurveTo.getY());
        }
        if (element instanceof ArcTo arcTo) {
            ArcTo copy = new ArcTo();
            copy.setX(arcTo.getX());
            copy.setY(arcTo.getY());
            copy.setRadiusX(arcTo.getRadiusX());
            copy.setRadiusY(arcTo.getRadiusY());
            copy.setXAxisRotation(arcTo.getXAxisRotation());
            copy.setLargeArcFlag(arcTo.isLargeArcFlag());
            copy.setSweepFlag(arcTo.isSweepFlag());
            return copy;
        }
        if (element instanceof ClosePath) {
            return new ClosePath();
        }
        throw new IllegalArgumentException("Unsupported PathElement type: " + element.getClass().getName());
    }

    private void updateScrollPaneViewportStyle() {
        Node viewport = scrollPane.lookup(".viewport");
        if (viewport instanceof Region region) {
            region.setBackground(Background.EMPTY);
            region.setStyle("-fx-background-color: transparent;");
        }

        ObservableList<Node> children = scrollPane.getChildrenUnmodifiable();
        for (Node child : children) {
            if (child instanceof Region region && child != viewport)
                region.setBackground(Background.EMPTY);
        }
    }

    private void rebuildVisibleTextRuns() {
        String content = getText();
        if (content == null || content.isEmpty()) {
            visibleTextFlow.getChildren().clear();
            return;
        }

        List<Integer> boundaries = new ArrayList<>();
        boundaries.add(0);
        boundaries.add(content.length());

        for (HighlightRegistry.Entry entry : highlights.entries()) {
            HighlightStyle style = palette.get(entry.name());
            if (style == null || !style.hasForeground())
                continue;

            for (HighlightRange range : entry.highlight().ranges()) {
                HighlightRange clamped = range.clampToLength(content.length());
                if (clamped.isCollapsed())
                    continue;
                boundaries.add(clamped.start());
                boundaries.add(clamped.end());
            }
        }

        Collections.sort(boundaries);
        List<Text> runs = new ArrayList<>();
        int previous = -1;
        for (int boundary : boundaries) {
            if (boundary == previous)
                continue;
            previous = boundary;
        }

        List<Integer> uniqueBoundaries = boundaries.stream().distinct().sorted().toList();
        for (int i = 0; i < uniqueBoundaries.size() - 1; i++) {
            int start = uniqueBoundaries.get(i);
            int end = uniqueBoundaries.get(i + 1);
            if (end <= start)
                continue;

            Paint fill = resolveForegroundFill(start, end);
            Text run = new Text(content.substring(start, end));
            run.setFont(getFont());
            run.setFontSmoothingType(getFontSmoothingType());
            run.setFill(fill);
            run.setBoundsType(TextBoundsType.LOGICAL);
            runs.add(run);
        }

        visibleTextFlow.getChildren().setAll(runs);
    }

    private Paint resolveForegroundFill(int start, int end) {
        HighlightRegistry.Entry winner = null;

        for (HighlightRegistry.Entry entry : highlights.entries()) {
            HighlightStyle style = palette.get(entry.name());
            if (style == null || !style.hasForeground())
                continue;
            if (!covers(entry.highlight(), start, end))
                continue;

            if (winner == null) {
                winner = entry;
                continue;
            }

            int compare = highlights.compareOverlayStackingPosition(
                winner.name(),
                winner.highlight(),
                entry.name(),
                entry.highlight());
            if (compare < 0)
                winner = entry;
        }

        if (winner == null)
            return getTextFill();

        HighlightStyle style = palette.get(winner.name());
        return style == null || !style.hasForeground() ? getTextFill() : style.foreground();
    }

    private boolean covers(Highlight highlight, int start, int end) {
        for (HighlightRange range : highlight.ranges()) {
            if (range.start() <= start && range.end() >= end)
                return true;
        }
        return false;
    }
}

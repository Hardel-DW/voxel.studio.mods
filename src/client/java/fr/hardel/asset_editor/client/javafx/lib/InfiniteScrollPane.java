package fr.hardel.asset_editor.client.javafx.lib;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Function;

public final class InfiniteScrollPane<T> extends ScrollPane {

    private final VBox content;
    private final Function<T, Node> factory;
    private final int batchSize;
    private List<T> items = List.of();
    private int visibleCount;

    public InfiniteScrollPane(int batchSize, Function<T, Node> factory) {
        this.batchSize = batchSize;
        this.factory = factory;
        this.visibleCount = batchSize;
        this.content = new VBox();

        setContent(content);
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);

        vvalueProperty().addListener((obs, o, v) -> onScroll());
        content.heightProperty().addListener((obs, o, v) -> checkAutoLoad());
    }

    public void setItems(List<T> items) {
        this.items = items;
        this.visibleCount = batchSize;
        rebuild();
        setVvalue(0);
    }

    public VBox content() {
        return content;
    }

    private void rebuild() {
        int end = Math.min(visibleCount, items.size());
        content.getChildren().clear();
        for (int i = 0; i < end; i++) {
            content.getChildren().add(factory.apply(items.get(i)));
        }
    }

    private void loadMore() {
        if (!hasMore())
            return;
        int oldEnd = Math.min(visibleCount, items.size());
        visibleCount += batchSize;
        int newEnd = Math.min(visibleCount, items.size());
        for (int i = oldEnd; i < newEnd; i++) {
            content.getChildren().add(factory.apply(items.get(i)));
        }
    }

    private boolean hasMore() {
        return visibleCount < items.size();
    }

    private void onScroll() {
        if (!hasMore())
            return;
        double contentHeight = content.getBoundsInLocal().getHeight();
        double viewportHeight = getViewportBounds().getHeight();
        if (viewportHeight <= 0 || contentHeight <= viewportHeight)
            return;
        double scrollBottom = getVvalue() * (contentHeight - viewportHeight) + viewportHeight;
        if (scrollBottom >= contentHeight - 100)
            loadMore();
    }

    private void checkAutoLoad() {
        if (!hasMore())
            return;
        double viewportHeight = getViewportBounds().getHeight();
        double contentHeight = content.getBoundsInLocal().getHeight();
        if (viewportHeight > 0 && contentHeight <= viewportHeight) {
            Platform.runLater(this::loadMore);
        }
    }
}

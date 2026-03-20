package fr.hardel.asset_editor.client.javafx.components.ui;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import java.util.function.IntConsumer;

public final class Pagination extends HBox {

    private int currentPage;
    private int totalPages;
    private IntConsumer onPageChange;

    public Pagination() {
        setAlignment(Pos.CENTER);
        setSpacing(4);
        getStyleClass().add("ui-pagination");
    }

    public void setPageCount(int totalPages) {
        this.totalPages = Math.max(1, totalPages);
        if (currentPage >= this.totalPages)
            currentPage = this.totalPages - 1;
        rebuild();
    }

    public void setCurrentPage(int page) {
        int clamped = Math.max(0, Math.min(page, totalPages - 1));
        if (clamped == currentPage) return;
        currentPage = clamped;
        rebuild();
        if (onPageChange != null) onPageChange.accept(currentPage);
    }

    public int currentPage() {
        return currentPage;
    }

    public int totalPages() {
        return totalPages;
    }

    public void setOnPageChange(IntConsumer listener) {
        this.onPageChange = listener;
    }

    private static final int SLOT_COUNT = 5;

    private void rebuild() {
        getChildren().clear();
        if (totalPages <= 1) return;

        getChildren().add(navButton("<", currentPage > 0, currentPage - 1));

        int last = totalPages - 1;
        int center = Math.max(2, Math.min(currentPage, last - 2));
        int[] pages = {0, center - 1, center, center + 1, last};

        for (int i = 0; i < SLOT_COUNT; i++) {
            if (i >= totalPages)
                getChildren().add(spacer());
            else if (i > 0 && pages[i] > pages[i - 1] + 1)
                getChildren().add(ellipsis());
            else
                getChildren().add(pageButton(pages[i]));
        }

        getChildren().add(navButton(">", currentPage < last, currentPage + 1));
    }

    private static Region spacer() {
        Region region = new Region();
        region.setPrefSize(28, 28);
        region.setMinSize(28, 28);
        region.setMaxSize(28, 28);
        return region;
    }

    private Label pageButton(int page) {
        Label label = new Label(String.valueOf(page + 1));
        label.setFont(VoxelFonts.of(VoxelFonts.Variant.MEDIUM, 12));
        label.setTextFill(page == currentPage ? VoxelColors.ZINC_100 : VoxelColors.ZINC_500);
        label.setPrefSize(28, 28);
        label.setAlignment(Pos.CENTER);
        label.setCursor(Cursor.HAND);

        if (page == currentPage)
            label.setStyle("-fx-background-color: #303033; -fx-background-radius: 6;");

        label.setOnMouseClicked(e -> setCurrentPage(page));
        return label;
    }

    private Label navButton(String text, boolean enabled, int targetPage) {
        Label label = new Label(text);
        label.setFont(VoxelFonts.of(VoxelFonts.Variant.MEDIUM, 12));
        label.setTextFill(enabled ? VoxelColors.ZINC_400 : VoxelColors.ZINC_700);
        label.setPrefSize(28, 28);
        label.setAlignment(Pos.CENTER);

        if (enabled) {
            label.setCursor(Cursor.HAND);
            label.setOnMouseClicked(e -> setCurrentPage(targetPage));
        }

        return label;
    }

    private static Label ellipsis() {
        Label label = new Label("…");
        label.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 12));
        label.setTextFill(VoxelColors.ZINC_600);
        label.setPrefSize(28, 28);
        label.setAlignment(Pos.CENTER);
        return label;
    }
}

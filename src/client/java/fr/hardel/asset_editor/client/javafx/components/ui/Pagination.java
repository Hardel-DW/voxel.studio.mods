package fr.hardel.asset_editor.client.javafx.components.ui;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.util.function.IntConsumer;

public final class Pagination extends HBox {

    private static final int VISIBLE = 5;

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

    private void rebuild() {
        getChildren().clear();
        if (totalPages <= 1) return;

        int count = Math.min(totalPages, VISIBLE);
        int half = count / 2;
        int start = Math.max(0, Math.min(currentPage - half, totalPages - count));

        getChildren().add(navButton("<", currentPage > 0, currentPage - 1));
        for (int i = 0; i < count; i++)
            getChildren().add(pageButton(start + i));
        getChildren().add(navButton(">", currentPage < totalPages - 1, currentPage + 1));
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
}

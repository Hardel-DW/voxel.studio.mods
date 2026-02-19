package fr.hardel.asset_editor.client.javafx.layout;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.*;
import javafx.scene.paint.*;

/**
 * Matches the GridBackground component from Splash.tsx.
 * squaring-zinc-800 opacity-20 + radial mask + radial overlay.
 */
public final class GridBackground extends StackPane {

    private static final double CELL_SIZE = 64;

    public GridBackground() {
        setMouseTransparent(true);

        Canvas grid = new Canvas();
        grid.widthProperty().bind(widthProperty());
        grid.heightProperty().bind(heightProperty());
        grid.widthProperty().addListener((obs, ov, nv) -> drawGrid(grid));
        grid.heightProperty().addListener((obs, ov, nv) -> drawGrid(grid));

        getChildren().addAll(grid, buildMaskOverlay(), buildRadialOverlay());
    }

    private void drawGrid(Canvas canvas) {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);
        gc.setStroke(VoxelColors.ZINC_800);
        gc.setLineWidth(1);
        gc.setGlobalAlpha(0.20);

        for (double x = 0; x <= w; x += CELL_SIZE) gc.strokeLine(x, 0, x, h);
        for (double y = 0; y <= h; y += CELL_SIZE) gc.strokeLine(0, y, w, y);
    }

    private Region buildMaskOverlay() {
        Region mask = new Region();
        mask.setBackground(new Background(new BackgroundFill(
                new RadialGradient(0, 0, 0.5, 0.5, 0.9, true, CycleMethod.NO_CYCLE,
                        new Stop(0.7, Color.TRANSPARENT),
                        new Stop(1.0, Color.BLACK)),
                null, null)));
        return mask;
    }

    private Region buildRadialOverlay() {
        Region overlay = new Region();
        overlay.setBackground(new Background(new BackgroundFill(
                new RadialGradient(0, 0, 0.5, 0.5, 1.0, true, CycleMethod.NO_CYCLE,
                        new Stop(0.0, VoxelColors.ZINC_900_50),
                        new Stop(0.5, Color.BLACK),
                        new Stop(1.0, Color.BLACK)),
                null, null)));
        return overlay;
    }
}

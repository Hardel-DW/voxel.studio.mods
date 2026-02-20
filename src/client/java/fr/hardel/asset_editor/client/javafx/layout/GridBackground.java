package fr.hardel.asset_editor.client.javafx.layout;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.*;
import javafx.scene.paint.*;

/**
 * Matches the GridBackground component from Splash.tsx.
 * Layer 1 – Canvas: squaring-zinc-800 opacity-20 with elliptical mask
 *   (mask-image: radial-gradient(ellipse 60% 50% at 50% 50%, #000 70%, transparent 100%))
 * Layer 2 – Region: bg-radial-at-c from-zinc-900/50 via-black to-black
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

        getChildren().add(grid);
    }

    private void drawGrid(Canvas canvas) {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        gc.setStroke(VoxelColors.ZINC_950);
        gc.setLineWidth(1);
        gc.setGlobalAlpha(1.0);
        for (double x = 0; x <= w; x += CELL_SIZE) gc.strokeLine(snap(x), 0, snap(x), h);
        for (double y = 0; y <= h; y += CELL_SIZE) gc.strokeLine(0, snap(y), w, snap(y));

        // Elliptical mask: CSS mask-image: radial-gradient(ellipse 60% 50% at 50% 50%, #000 70%, transparent 100%)
        gc.setGlobalAlpha(1.0);
        double rx = 0.6 * w;
        double ry = 0.5 * h;
        gc.save();
        gc.transform(rx, 0, 0, ry, w / 2, h / 2);
        Color dim = new Color(0, 0, 0, 0.6);
        gc.setFill(new RadialGradient(0, 0, 0, 0, 1.0, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.TRANSPARENT),
                new Stop(0.4, Color.TRANSPARENT),
                new Stop(1.0, dim)));
        gc.fillRect(-w / (2 * rx), -h / (2 * ry), w / rx, h / ry);
        gc.restore();
    }

    private static double snap(double v) {
        return Math.floor(v) + 0.5;
    }
}

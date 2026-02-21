package fr.hardel.asset_editor.client.javafx.components.layout.editor;

import fr.hardel.asset_editor.client.javafx.context.StudioContext;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeType;
import javafx.stage.Stage;

public final class StudioEditorRoot extends HBox {

    private final StudioContext context = new StudioContext();

    public StudioEditorRoot(Stage stage) {
        getStyleClass().add("studio-root");

        StudioPrimarySidebar sidebar = new StudioPrimarySidebar(context);
        StudioEditorTabsBar header = new StudioEditorTabsBar(context, stage);

        StackPane contentBody = new StackPane(new EnchantmentLayout(context));
        contentBody.getStyleClass().add("studio-content-body");

        Path frame = new Path();
        frame.getStyleClass().add("studio-content-frame");
        frame.setMouseTransparent(true);
        frame.setManaged(false);
        frame.setFill(Color.TRANSPARENT);
        frame.setStrokeType(StrokeType.CENTERED);
        frame.setStrokeLineCap(StrokeLineCap.BUTT);

        StackPane contentSurface = new StackPane(contentBody, frame);
        contentSurface.getStyleClass().add("studio-content-surface");
        VBox.setVgrow(contentSurface, Priority.ALWAYS);

        // Rounded top-left mask + explicit border path to avoid JavaFX border-radius artifacts.
        contentSurface.layoutBoundsProperty().addListener((obs, oldBounds, bounds) ->
                refreshSurfaceGeometry(bounds.getWidth(), bounds.getHeight(), contentBody, frame));

        VBox workspace = new VBox(header, contentSurface);
        workspace.getStyleClass().add("studio-workspace");
        HBox.setHgrow(workspace, Priority.ALWAYS);

        getChildren().addAll(sidebar, workspace);
    }

    private static void refreshSurfaceGeometry(double width, double height, StackPane contentBody, Path frame) {
        contentBody.setClip(buildTlClip(width, height));
        frame.getElements().setAll(buildTlFrame(width, height).getElements());
    }

    private static Path buildTlClip(double width, double height) {
        if (width <= 0 || height <= 0)
            return new Path();
        double r = Math.min(24, Math.min(width, height));
        return new Path(
                new MoveTo(r, 0),
                new LineTo(width, 0),
                new LineTo(width, height),
                new LineTo(0, height),
                new LineTo(0, r),
                new QuadCurveTo(0, 0, r, 0),
                new ClosePath());
    }

    private static Path buildTlFrame(double width, double height) {
        if (width <= 0 || height <= 0)
            return new Path();
        double inset = 0.5;
        double w = Math.max(inset, width - inset);
        double h = Math.max(inset, height - inset);
        double r = Math.max(0, Math.min(24, Math.min(w, h)) - inset);
        return new Path(
                new MoveTo(inset + r, inset),
                new QuadCurveTo(inset, inset, inset, inset + r),
                new LineTo(inset, h),
                new MoveTo(inset + r, inset),
                new LineTo(w, inset));
    }
}




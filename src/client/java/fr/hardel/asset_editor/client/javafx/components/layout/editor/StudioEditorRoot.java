package fr.hardel.asset_editor.client.javafx.components.layout.editor;

import fr.hardel.asset_editor.client.javafx.context.StudioContext;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.stage.Stage;

public final class StudioEditorRoot extends HBox {

    private final StudioContext context = new StudioContext();

    public StudioEditorRoot(Stage stage) {
        getStyleClass().add("studio-root");

        StudioPrimarySidebar sidebar = new StudioPrimarySidebar(context);
        StudioEditorTabsBar header = new StudioEditorTabsBar(context, stage);

        StackPane contentSurface = new StackPane(new EnchantmentLayout(context));
        contentSurface.getStyleClass().add("studio-content-surface");
        VBox.setVgrow(contentSurface, Priority.ALWAYS);

        // Clip to rounded-tl-3xl (24px top-left radius only)
        contentSurface.widthProperty().addListener((obs, o, w) ->
                contentSurface.setClip(buildTlClip(w.doubleValue(), contentSurface.getHeight())));
        contentSurface.heightProperty().addListener((obs, o, h) ->
                contentSurface.setClip(buildTlClip(contentSurface.getWidth(), h.doubleValue())));

        VBox workspace = new VBox(header, contentSurface);
        workspace.getStyleClass().add("studio-workspace");
        HBox.setHgrow(workspace, Priority.ALWAYS);

        getChildren().addAll(sidebar, workspace);
    }

    private static Path buildTlClip(double width, double height) {
        double r = 24;
        return new Path(
                new MoveTo(r, 0),
                new LineTo(width, 0),
                new LineTo(width, height),
                new LineTo(0, height),
                new LineTo(0, r),
                new ArcTo(r, r, 0, r, 0, false, true),
                new ClosePath());
    }
}




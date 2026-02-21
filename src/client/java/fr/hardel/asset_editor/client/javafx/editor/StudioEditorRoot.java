package fr.hardel.asset_editor.client.javafx.editor;

import fr.hardel.asset_editor.client.javafx.editor.layout.EnchantmentLayout;
import fr.hardel.asset_editor.client.javafx.editor.layout.StudioEditorTabsBar;
import fr.hardel.asset_editor.client.javafx.editor.layout.StudioPrimarySidebar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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

        VBox workspace = new VBox(header, contentSurface);
        workspace.getStyleClass().add("studio-workspace");
        HBox.setHgrow(workspace, Priority.ALWAYS);

        getChildren().addAll(sidebar, workspace);
    }
}

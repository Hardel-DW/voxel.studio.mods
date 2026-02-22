package fr.hardel.asset_editor.client.javafx.components.layout.editor;

import fr.hardel.asset_editor.client.javafx.routes.changes.ChangesMainPage;
import javafx.scene.layout.StackPane;

public final class ChangesLayout extends StackPane {

    public ChangesLayout() {
        getStyleClass().add("enchantment-layout");
        getChildren().add(new ChangesMainPage());
    }
}

package fr.hardel.asset_editor.client.javafx.routes.changes;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import net.minecraft.client.resources.language.I18n;

public final class ChangesMainPage extends StackPane {

    public ChangesMainPage() {
        getStyleClass().add("concept-main-page");
        setAlignment(Pos.CENTER);
        Label label = new Label(I18n.get("changes:layout.title"));
        label.getStyleClass().add("concept-main-placeholder");
        getChildren().add(label);
    }
}

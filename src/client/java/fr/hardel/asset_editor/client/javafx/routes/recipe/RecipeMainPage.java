package fr.hardel.asset_editor.client.javafx.routes.recipe;

import fr.hardel.asset_editor.client.javafx.lib.Page;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import net.minecraft.client.resources.language.I18n;

public final class RecipeMainPage extends StackPane implements Page {

    public RecipeMainPage() {
        getStyleClass().add("concept-main-page");
        setAlignment(Pos.CENTER);
        Label label = new Label(I18n.get("studio.coming_soon.title"));
        label.getStyleClass().add("concept-main-placeholder");
        getChildren().add(label);
    }
}

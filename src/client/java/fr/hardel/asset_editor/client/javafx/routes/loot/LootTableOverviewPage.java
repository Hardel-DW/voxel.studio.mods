package fr.hardel.asset_editor.client.javafx.routes.loot;

import fr.hardel.asset_editor.client.javafx.context.StudioContext;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import net.minecraft.client.resources.language.I18n;

public final class LootTableOverviewPage extends VBox {

    public LootTableOverviewPage(StudioContext context) {
        getStyleClass().add("concept-overview-page");

        TextField search = new TextField();
        search.getStyleClass().add("concept-overview-search");
        search.setPromptText(I18n.get("loot:overview.search"));
        search.textProperty().bindBidirectional(context.uiState().searchProperty());

        VBox toolbar = new VBox(search);
        toolbar.getStyleClass().add("concept-overview-toolbar");
        toolbar.setPadding(new Insets(16, 32, 16, 32));

        Label title = new Label(I18n.get("loot:overview.empty.title"));
        title.getStyleClass().add("concept-overview-empty-title");
        Label body = new Label(I18n.get("loot:overview.empty.description"));
        body.getStyleClass().add("concept-overview-empty-body");
        body.setWrapText(true);

        VBox empty = new VBox(6, title, body);
        empty.getStyleClass().add("concept-overview-empty");

        ScrollPane scrollPane = new ScrollPane(empty);
        scrollPane.getStyleClass().add("concept-overview-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().addAll(toolbar, scrollPane);
    }
}

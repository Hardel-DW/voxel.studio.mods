package fr.hardel.asset_editor.client.javafx.components.layout.editor;

import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.components.ui.ResourceImageIcon;
import fr.hardel.asset_editor.client.javafx.components.ui.SvgIcon;
import fr.hardel.asset_editor.client.javafx.components.ui.tree.TreeController;
import fr.hardel.asset_editor.client.javafx.components.ui.tree.TreeSidebarView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;
import fr.hardel.asset_editor.client.javafx.lib.utils.BrowserUtils;

import java.util.List;

public final class EditorSidebar extends VBox {

    private static final Identifier DISCORD_ICON = Identifier.fromNamespaceAndPath("asset_editor", "icons/company/discord.svg");

    public EditorSidebar(StudioContext context, TreeController tree, String titleKey, Identifier iconPath, List<Node> topContent) {
        getStyleClass().add("editor-sidebar");

        Button title = new Button(I18n.get(titleKey));
        title.getStyleClass().add("editor-sidebar-title");
        title.setGraphic(new ResourceImageIcon(iconPath, 20));
        title.setOnAction(event -> tree.selectAll());

        Label subtitle = new Label(I18n.get("explore"));
        subtitle.getStyleClass().add("editor-sidebar-subtitle");

        VBox header = new VBox(2, title, subtitle);
        header.setPadding(new Insets(24, 24, 0, 24));

        VBox content = new VBox();
        content.getStyleClass().add("editor-sidebar-content");
        content.setPadding(new Insets(0, 12, 0, 12));
        content.setSpacing(0);
        if (topContent != null && !topContent.isEmpty()) {
            content.getChildren().addAll(topContent);
        }
        TreeSidebarView treeSidebar = new TreeSidebarView(context, tree);
        content.getChildren().add(treeSidebar);
        VBox.setVgrow(treeSidebar, Priority.ALWAYS);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.getStyleClass().add("editor-sidebar-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().addAll(header, scrollPane, buildDiscordSection());
    }

    private Node buildDiscordSection() {
        Label text = new Label(I18n.get("common.help.discord"));
        text.getStyleClass().add("editor-sidebar-discord-text");
        HBox.setHgrow(text, Priority.ALWAYS);

        SvgIcon discordIcon = new SvgIcon(DISCORD_ICON, 16, Color.WHITE);
        discordIcon.setOpacity(0.3);

        StackPane circle = new StackPane(discordIcon);
        circle.getStyleClass().add("editor-sidebar-discord-circle");
        circle.setPrefSize(32, 32);
        circle.setMinSize(32, 32);
        circle.setMaxSize(32, 32);
        circle.setAlignment(Pos.CENTER);

        HBox card = new HBox(12, text, circle);
        card.getStyleClass().add("editor-sidebar-discord-card");
        card.setPadding(new Insets(12));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setCursor(Cursor.HAND);
        card.setOnMouseEntered(event -> {
            card.getStyleClass().add("editor-sidebar-discord-card-hover");
            circle.getStyleClass().add("editor-sidebar-discord-circle-hover");
            circle.getStyleClass().remove("editor-sidebar-discord-circle");
            discordIcon.setOpacity(0.5);
        });
        card.setOnMouseExited(event -> {
            card.getStyleClass().remove("editor-sidebar-discord-card-hover");
            circle.getStyleClass().remove("editor-sidebar-discord-circle-hover");
            circle.getStyleClass().add("editor-sidebar-discord-circle");
            discordIcon.setOpacity(0.3);
        });
        card.setOnMouseClicked(event -> BrowserUtils.openBrowser("https://discord.gg/TAmVFvkHep"));

        VBox wrapper = new VBox(card);
        wrapper.getStyleClass().add("editor-sidebar-discord-wrapper");
        wrapper.setPadding(new Insets(16));
        return wrapper;
    }

}

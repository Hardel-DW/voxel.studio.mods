package fr.hardel.asset_editor.client.javafx.editor.layout;

import fr.hardel.asset_editor.client.javafx.editor.StudioContext;
import fr.hardel.asset_editor.client.javafx.editor.state.StudioConcept;
import fr.hardel.asset_editor.client.javafx.editor.state.StudioRoute;
import fr.hardel.asset_editor.client.javafx.editor.state.StudioSidebarView;
import fr.hardel.asset_editor.client.javafx.ui.ResourceImageIcon;
import fr.hardel.asset_editor.client.javafx.ui.SvgIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

/**
 * Left sidebar inside the enchantment layout.
 * Matches EditorSidebar.tsx: title, explore subtitle, toggle group, tree,
 * discord footer.
 */
public final class EnchantmentSidebar extends VBox {

    private static final Identifier DISCORD_ICON = Identifier.fromNamespaceAndPath("asset_editor",
            "icons/company/discord.svg");

    private final StudioContext context;

    public EnchantmentSidebar(StudioContext context) {
        this.context = context;
        getStyleClass().add("enchantment-sidebar");

        Button title = new Button(I18n.get("enchantment:overview.title"));
        title.getStyleClass().add("enchantment-sidebar-title");
        title.setGraphic(new ResourceImageIcon(StudioConcept.ENCHANTMENT.icon(), 20)); // size-5
        title.setOnAction(e -> context.router().navigate(StudioRoute.ENCHANTMENT_OVERVIEW));

        Label subtitle = new Label(I18n.get("explore"));
        subtitle.getStyleClass().add("enchantment-sidebar-subtitle");

        HBox toggle = new HBox(6,
                toggleButton(StudioSidebarView.SLOTS),
                toggleButton(StudioSidebarView.ITEMS),
                toggleButton(StudioSidebarView.EXCLUSIVE));
        toggle.getStyleClass().add("enchantment-sidebar-toggle");
        toggle.setPadding(new Insets(16, 0, 0, 0)); // mt-4

        EnchantmentTreeSidebar tree = new EnchantmentTreeSidebar(context);
        VBox.setVgrow(tree, Priority.ALWAYS);

        VBox top = new VBox(2, title, subtitle, toggle);
        top.setPadding(new Insets(24, 24, 10, 24)); // px-6 pt-6

        getChildren().addAll(top, tree, buildDiscordSection());
    }

    private Button toggleButton(StudioSidebarView mode) {
        Button button = new Button(I18n.get(mode.translationKey()));
        button.getStyleClass().add("enchantment-sidebar-toggle-button");
        if (context.uiState().sidebarView() == mode)
            button.getStyleClass().add("enchantment-sidebar-toggle-button-active");
        button.setOnAction(e -> {
            context.uiState().setSidebarView(mode);
            context.uiState().setFilterPath("");
        });
        context.uiState().sidebarViewProperty().addListener((obs, oldVal, newVal) -> {
            button.getStyleClass().remove("enchantment-sidebar-toggle-button-active");
            if (newVal == mode)
                button.getStyleClass().add("enchantment-sidebar-toggle-button-active");
        });
        return button;
    }

    /**
     * "Need Help?" Discord footer card.
     * Matches EditorSidebar.tsx bottom section:
     * p-4 border-t bg-zinc-950/90 > a.rounded-lg.p-3.border [text | icon circle]
     */
    private VBox buildDiscordSection() {
        Label text = new Label(I18n.get("common.help.discord"));
        text.getStyleClass().add("enchantment-sidebar-discord-text");
        text.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(text, Priority.ALWAYS);

        SvgIcon discordIcon = new SvgIcon(DISCORD_ICON, 16, Color.WHITE); // size-4
        discordIcon.setOpacity(0.3);

        StackPane circle = new StackPane(discordIcon);
        circle.getStyleClass().add("enchantment-sidebar-discord-circle");
        circle.setPrefSize(32, 32); // size-8
        circle.setMinSize(32, 32);
        circle.setMaxSize(32, 32);
        circle.setAlignment(Pos.CENTER);

        HBox card = new HBox(12, text, circle); // gap-3
        card.getStyleClass().add("enchantment-sidebar-discord-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12)); // p-3
        card.setCursor(Cursor.HAND);

        card.setOnMouseEntered(e -> {
            card.getStyleClass().add("enchantment-sidebar-discord-card-hover");
            circle.getStyleClass().add("enchantment-sidebar-discord-circle-hover");
            circle.getStyleClass().remove("enchantment-sidebar-discord-circle");
            discordIcon.setOpacity(0.5);
            text.setStyle("-fx-text-fill: white;");
        });
        card.setOnMouseExited(e -> {
            card.getStyleClass().remove("enchantment-sidebar-discord-card-hover");
            circle.getStyleClass().remove("enchantment-sidebar-discord-circle-hover");
            circle.getStyleClass().add("enchantment-sidebar-discord-circle");
            discordIcon.setOpacity(0.3);
            text.setStyle("");
        });
        card.setOnMouseClicked(e -> openBrowser("https://discord.gg/TAmVFvkHep"));

        VBox wrapper = new VBox(card);
        wrapper.getStyleClass().add("enchantment-sidebar-discord-wrapper");
        wrapper.setPadding(new Insets(16)); // p-4
        return wrapper;
    }

    private static void openBrowser(String url) {
        try {
            Runtime.getRuntime().exec(new String[] { "rundll32", "url.dll,FileProtocolHandler", url });
        } catch (Exception ignored) {
        }
    }
}

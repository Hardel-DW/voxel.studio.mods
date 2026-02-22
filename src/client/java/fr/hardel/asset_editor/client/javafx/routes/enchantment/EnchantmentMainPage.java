package fr.hardel.asset_editor.client.javafx.routes.enchantment;

import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.data.mock.StudioMockEnchantment;
import fr.hardel.asset_editor.client.javafx.lib.utils.BrowserUtils;
import fr.hardel.asset_editor.client.javafx.components.ui.SvgIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

public final class EnchantmentMainPage extends VBox {

    private static final Identifier LOGO  = Identifier.fromNamespaceAndPath("asset_editor", "icons/logo.svg");
    private static final Identifier CHECK = Identifier.fromNamespaceAndPath("asset_editor", "icons/check.svg");
    private static final String[] ADVANTAGES = {"early_access", "submit_ideas", "discord_role", "live_voxel"};

    private final StudioContext context;
    private final VBox content = new VBox(32);

    public EnchantmentMainPage(StudioContext context) {
        this.context = context;
        getStyleClass().add("enchantment-main-page");

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("enchantment-main-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().add(scroll);

        content.setPadding(new Insets(16, 32, 28, 32));
        context.tabsState().currentElementIdProperty().addListener((obs, oldValue, newValue) -> refresh());
        refresh();
    }

    private void refresh() {
        StudioMockEnchantment selected = selectedEnchantment();

        GridPane cards = new GridPane();
        cards.setHgap(16);
        cards.setVgap(16);
        for (int i = 0; i < 3; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setHgrow(Priority.ALWAYS);
            c.setFillWidth(true);
            cards.getColumnConstraints().add(c);
        }
        cards.add(toolCard(I18n.get("enchantment:global.maxLevel.title"), I18n.get("enchantment:global.explanation.list.1"), String.valueOf(selected.maxLevel())), 0, 0);
        cards.add(toolCard(I18n.get("enchantment:global.weight.title"), I18n.get("enchantment:global.explanation.list.2"), String.valueOf(selected.weight())), 1, 0);
        cards.add(toolCard(I18n.get("enchantment:global.anvilCost.title"), I18n.get("enchantment:global.explanation.list.3"), String.valueOf(selected.anvilCost())), 2, 0);

        Label sectionTitle = new Label(I18n.get("enchantment:section.global.description"));
        sectionTitle.getStyleClass().add("enchantment-main-section-title");

        VBox section = new VBox(12, sectionTitle, cards, modeRow());
        section.getStyleClass().add("enchantment-main-section");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        content.getChildren().setAll(section, spacer, supportCard());
    }

    private VBox toolCard(String title, String description, String value) {
        Label cardTitle = new Label(title);
        cardTitle.getStyleClass().add("enchantment-main-card-title");
        Label cardDescription = new Label(description);
        cardDescription.getStyleClass().add("enchantment-main-card-description");
        cardDescription.setWrapText(true);
        Label cardValue = new Label(value);
        cardValue.getStyleClass().add("enchantment-main-card-value");
        VBox card = new VBox(8, cardTitle, cardDescription, cardValue);
        card.getStyleClass().add("enchantment-main-card");
        card.setPadding(new Insets(14));
        return card;
    }

    private HBox modeRow() {
        Label title = new Label(I18n.get("enchantment:global.mode.title"));
        title.getStyleClass().add("enchantment-main-mode-title");
        Label description = new Label(I18n.get("enchantment:global.mode.description"));
        description.getStyleClass().add("enchantment-main-mode-description");
        HBox row = new HBox(new VBox(2, title, description));
        row.getStyleClass().add("enchantment-main-mode-row");
        return row;
    }

    private StackPane supportCard() {
        SvgIcon logo = new SvgIcon(LOGO, 384, Color.WHITE);
        logo.setOpacity(0.2);
        StackPane.setAlignment(logo, Pos.TOP_RIGHT);
        logo.setTranslateX(96);
        logo.setTranslateY(-96);

        Label title = new Label(I18n.get("supports.title"));
        title.getStyleClass().add("enchantment-support-title");
        title.setWrapText(true);

        Label description = new Label(I18n.get("supports.description"));
        description.getStyleClass().add("enchantment-support-description");
        description.setWrapText(true);
        description.setPadding(new Insets(8, 0, 0, 0));

        Label heading = new Label(I18n.get("supports.advantages"));
        heading.getStyleClass().add("enchantment-support-heading");
        heading.setPadding(new Insets(24, 0, 16, 0));

        GridPane grid = new GridPane();
        grid.setHgap(32);
        grid.setVgap(16);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(c);
        }
        for (int i = 0; i < ADVANTAGES.length; i++)
            grid.add(advantageItem("supports.advantages." + ADVANTAGES[i]), i % 2, i / 2);

        VBox advantagesSection = new VBox(heading, grid);
        HBox.setHgrow(advantagesSection, Priority.ALWAYS);

        Label donate = new Label(I18n.get("donate"));
        donate.getStyleClass().add("enchantment-support-action");
        donate.setCursor(Cursor.HAND);
        donate.setOnMouseClicked(e -> BrowserUtils.openBrowser("https://streamelements.com/hardoudou/tip"));

        Label patreon = new Label(I18n.get("supports.become"));
        patreon.getStyleClass().add("enchantment-support-action-patreon");
        patreon.setCursor(Cursor.HAND);
        patreon.setOnMouseClicked(e -> BrowserUtils.openBrowser("https://www.patreon.com/hardel"));

        VBox actionsSection = new VBox(16, donate, patreon);
        actionsSection.setAlignment(Pos.BOTTOM_LEFT);

        HBox bottom = new HBox(32, advantagesSection, actionsSection);
        bottom.setPadding(new Insets(16, 0, 0, 0));

        VBox content = new VBox(title, description, bottom);
        content.setPadding(new Insets(32, 32, 32, 48));
        StackPane.setAlignment(content, Pos.TOP_LEFT);

        StackPane card = new StackPane(logo, content);
        card.setAlignment(Pos.TOP_LEFT);
        card.getStyleClass().add("enchantment-support-card");

        Rectangle clip = new Rectangle();
        clip.setArcWidth(32);
        clip.setArcHeight(32);
        card.widthProperty().addListener((obs, o, w) -> clip.setWidth(w.doubleValue()));
        card.heightProperty().addListener((obs, o, h) -> clip.setHeight(h.doubleValue()));
        card.setClip(clip);

        return card;
    }

    private HBox advantageItem(String key) {
        SvgIcon check = new SvgIcon(CHECK, 16, Color.WHITE);
        Label text = new Label(I18n.get(key));
        text.getStyleClass().add("enchantment-support-benefit");
        HBox item = new HBox(8, check, text);
        item.setAlignment(Pos.CENTER_LEFT);
        return item;
    }

    private StudioMockEnchantment selectedEnchantment() {
        String id = context.tabsState().currentElementId();
        if (!id.isBlank()) {
            for (StudioMockEnchantment enchantment : context.repository().enchantments()) {
                if (enchantment.uniqueKey().equals(id))
                    return enchantment;
            }
        }
        return context.repository().enchantments().getFirst();
    }
}


package fr.hardel.asset_editor.client.javafx.routes.enchantment;

import fr.hardel.asset_editor.client.javafx.ResourceLoader;
import fr.hardel.asset_editor.client.javafx.components.page.enchantment.TemplateCard;
import fr.hardel.asset_editor.client.javafx.components.page.enchantment.ToolSelector;
import fr.hardel.asset_editor.client.javafx.components.ui.Counter;
import fr.hardel.asset_editor.client.javafx.components.ui.StudioButton;
import fr.hardel.asset_editor.client.javafx.components.ui.SvgIcon;
import fr.hardel.asset_editor.client.javafx.components.ui.ToolSection;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.data.mock.StudioMockEnchantment;
import fr.hardel.asset_editor.client.javafx.lib.utils.BrowserUtils;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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

import java.util.LinkedHashMap;

public final class EnchantmentMainPage extends VBox {

    private static final Identifier LOGO = Identifier.fromNamespaceAndPath("asset_editor", "icons/logo.svg");
    private static final Identifier SHINE = Identifier.fromNamespaceAndPath("asset_editor", "textures/studio/shine.png");
    private static final Identifier CHECK = Identifier.fromNamespaceAndPath("asset_editor", "icons/check.svg");
    private static final Identifier MAX_LEVEL_ICON = Identifier.fromNamespaceAndPath("asset_editor",
            "icons/tools/max_level.svg");
    private static final Identifier WEIGHT_ICON = Identifier.fromNamespaceAndPath("asset_editor",
            "icons/tools/weight.svg");
    private static final Identifier ANVIL_COST_ICON = Identifier.fromNamespaceAndPath("asset_editor",
            "icons/tools/anvil_cost.svg");
    private static final Identifier PATREON_ICON = Identifier.fromNamespaceAndPath("asset_editor",
            "icons/company/patreon.svg");

    private static final String[] ADVANTAGES = { "early_access", "submit_ideas", "discord_role", "live_voxel" };

    private final StudioContext context;
    private final VBox content = new VBox(32); // gap-8 = 32px

    public EnchantmentMainPage(StudioContext context) {
        this.context = context;

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("enchantment-main-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().add(scroll);

        // py-4 px-8 = 16px top/bottom, 32px left/right
        content.setPadding(new Insets(16, 32, 28, 32));
        scroll.viewportBoundsProperty().addListener((obs, o, bounds) ->
            content.setMinHeight(Math.max(0, bounds.getHeight())));

        context.tabsState().currentElementIdProperty().addListener((obs, o, v) -> refresh());
        refresh();
    }

    private void refresh() {
        StudioMockEnchantment selected = selectedEnchantment();

        // ToolSection with 3 TemplateCards + ToolSelector
        ToolSection section = new ToolSection("enchantment:section.global.description");

        // grid-auto-64 max-xl:grid-cols-1 → 3 equal columns
        GridPane cardsGrid = buildCardsGrid(selected);
        section.addContent(cardsGrid, buildModeSelector());

        // mt-auto support card (pushed to bottom of flex column)
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        content.getChildren().setAll(section, spacer, buildSupportCard());
    }

    private GridPane buildCardsGrid(StudioMockEnchantment e) {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);
        for (int i = 0; i < 3; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setHgrow(Priority.ALWAYS);
            c.setFillWidth(true);
            grid.getColumnConstraints().add(c);
        }
        grid.add(new TemplateCard(MAX_LEVEL_ICON, "enchantment:global.maxLevel.title",
                "enchantment:global.explanation.list.1", new Counter(1, 127, 1, e.maxLevel())), 0, 0);
        grid.add(new TemplateCard(WEIGHT_ICON, "enchantment:global.weight.title",
                "enchantment:global.explanation.list.2", new Counter(1, 1024, 1, e.weight())), 1, 0);
        grid.add(new TemplateCard(ANVIL_COST_ICON, "enchantment:global.anvilCost.title",
                "enchantment:global.explanation.list.3", new Counter(0, 255, 1, e.anvilCost())), 2, 0);
        return grid;
    }

    private ToolSelector buildModeSelector() {
        LinkedHashMap<String, String> modeOptions = new LinkedHashMap<>();
        modeOptions.put("normal", I18n.get("enchantment:global.mode.enum.normal"));
        modeOptions.put("soft_delete", I18n.get("enchantment:global.mode.enum.soft_delete"));
        modeOptions.put("only_creative", I18n.get("enchantment:global.mode.enum.only_creative"));

        return new ToolSelector(
                "enchantment:global.mode.title",
                "enchantment:global.mode.description",
                modeOptions, "normal", v -> {
                });
    }

    private StackPane buildSupportCard() {
        // Decorative logo: absolute -top-24 -right-24 size-96 opacity-20
        SvgIcon logo = new SvgIcon(LOGO, 384, Color.WHITE);
        logo.setOpacity(0.2);
        logo.setManaged(false);
        logo.setMouseTransparent(true);

        // Title + description
        Label title = new Label(I18n.get("supports.title"));
        title.getStyleClass().add("support-card-title");
        title.setWrapText(true);

        Label desc = new Label(I18n.get("supports.description"));
        desc.getStyleClass().add("support-card-desc");
        desc.setWrapText(true);
        VBox.setMargin(desc, new Insets(8, 0, 0, 0)); // pt-2

        VBox textBlock = new VBox(title, desc);

        // Advantages heading + 2-column grid
        Label advantagesHeading = new Label(I18n.get("supports.advantages"));
        advantagesHeading.getStyleClass().add("support-card-advantages-heading");
        VBox.setMargin(advantagesHeading, new Insets(24, 0, 16, 0)); // pt-6 pb-4

        GridPane advantagesGrid = buildAdvantagesGrid();
        VBox advantagesSection = new VBox(advantagesHeading, advantagesGrid);
        HBox.setHgrow(advantagesSection, Priority.ALWAYS);

        // Buttons: flex-row gap-4, self-end
        StudioButton donateBtn = new StudioButton(StudioButton.Variant.SHIMMER, StudioButton.Size.LG,
                I18n.get("donate"));
        donateBtn.setOnAction(() -> BrowserUtils.openBrowser("https://streamelements.com/hardoudou/tip"));

        SvgIcon patreonIcon = new SvgIcon(PATREON_ICON, 16, Color.WHITE);
        StudioButton patreonBtn = new StudioButton(StudioButton.Variant.PATREON, StudioButton.Size.LG,
                I18n.get("supports.become"), patreonIcon);
        patreonBtn.setOnAction(() -> BrowserUtils.openBrowser("https://www.patreon.com/hardel"));
        donateBtn.setMaxWidth(Double.MAX_VALUE);
        patreonBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(donateBtn, Priority.ALWAYS);
        HBox.setHgrow(patreonBtn, Priority.ALWAYS);

        HBox buttonsRow = new HBox(16, donateBtn, patreonBtn);
        buttonsRow.setAlignment(Pos.CENTER_RIGHT);
        buttonsRow.setPadding(new Insets(32, 0, 0, 0)); // pt-8

        // self-end: push buttons to bottom of their column
        Region btnSpacer = new Region();
        VBox.setVgrow(btnSpacer, Priority.ALWAYS);
        VBox buttonsWrapper = new VBox(btnSpacer, buttonsRow);
        buttonsWrapper.setAlignment(Pos.BOTTOM_RIGHT);

        HBox bottom = new HBox(16, advantagesSection, buttonsWrapper);
        bottom.setPadding(new Insets(16, 0, 0, 0)); // mt-4

        VBox cardContent = new VBox(textBlock, bottom);
        cardContent.setPadding(new Insets(32, 32, 32, 48)); // p-8 pl-12
        StackPane.setAlignment(cardContent, Pos.TOP_LEFT);

        StackPane card = new StackPane();
        card.getStyleClass().add("support-card");
        card.setAlignment(Pos.TOP_LEFT);

        // Shine: absolute inset-0, brightness-15 ≈ opacity 0.15 (behind logo and content)
        try (var stream = ResourceLoader.open(SHINE)) {
            ImageView shine = new ImageView(new Image(stream));
            ColorAdjust shineEffect = new ColorAdjust();
            shineEffect.setHue(0.5);
            shineEffect.setBrightness(-0.8);
            shine.setEffect(shineEffect);
            shine.setPreserveRatio(false);
            shine.setManaged(false);
            shine.setMouseTransparent(true);
            shine.fitWidthProperty().bind(card.widthProperty());
            shine.fitHeightProperty().bind(card.heightProperty());
            card.getChildren().add(shine);
        } catch (Exception ignored) {}

        logo.layoutXProperty().bind(card.widthProperty().subtract(288));
        logo.setLayoutY(-96);
        card.getChildren().addAll(logo, cardContent);

        Rectangle clip = new Rectangle();
        clip.setArcWidth(32);
        clip.setArcHeight(32);
        card.widthProperty().addListener((obs, o, w) -> clip.setWidth(w.doubleValue()));
        card.heightProperty().addListener((obs, o, h) -> clip.setHeight(h.doubleValue()));
        card.setClip(clip);

        return card;
    }

    private GridPane buildAdvantagesGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(32); // gap-x-8 = 32px
        grid.setVgap(16); // gap-y-4 = 16px
        for (int i = 0; i < 2; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(c);
        }
        for (int i = 0; i < ADVANTAGES.length; i++) {
            grid.add(buildAdvantageItem("supports.advantages." + ADVANTAGES[i]), i % 2, i / 2);
        }
        return grid;
    }

    private HBox buildAdvantageItem(String key) {
        SvgIcon check = new SvgIcon(CHECK, 16, Color.WHITE);
        Label text = new Label(I18n.get(key));
        text.getStyleClass().add("support-card-benefit");
        HBox item = new HBox(8, check, text);
        item.setAlignment(Pos.CENTER_LEFT);
        return item;
    }

    private StudioMockEnchantment selectedEnchantment() {
        String id = context.tabsState().currentElementId();
        if (!id.isBlank()) {
            for (StudioMockEnchantment e : context.repository().enchantments()) {
                if (e.uniqueKey().equals(id))
                    return e;
            }
        }
        return context.repository().enchantments().getFirst();
    }
}

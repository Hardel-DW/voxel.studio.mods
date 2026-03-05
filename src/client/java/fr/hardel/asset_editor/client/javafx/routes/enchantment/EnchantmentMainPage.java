package fr.hardel.asset_editor.client.javafx.routes.enchantment;

import fr.hardel.asset_editor.client.javafx.ResourceLoader;
import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.components.ui.TemplateCard;
import fr.hardel.asset_editor.client.javafx.components.ui.Selector;
import fr.hardel.asset_editor.client.javafx.components.layout.editor.PackCreateDialog;
import fr.hardel.asset_editor.client.javafx.components.ui.Dialog;
import fr.hardel.asset_editor.client.javafx.components.ui.ResponsiveGrid;
import fr.hardel.asset_editor.client.javafx.components.ui.Counter;
import fr.hardel.asset_editor.client.javafx.components.ui.Button;
import fr.hardel.asset_editor.client.javafx.components.ui.SvgIcon;
import fr.hardel.asset_editor.client.javafx.components.ui.Section;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioBreakpoint;
import fr.hardel.asset_editor.client.javafx.lib.editor.action.EditorAction;
import fr.hardel.asset_editor.client.javafx.lib.utils.BrowserUtils;
import java.util.function.UnaryOperator;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
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
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;

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
    private final VBox content = new VBox(32);

    public EnchantmentMainPage(StudioContext context) {
        this.context = context;

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("enchantment-main-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().add(scroll);

        content.setPadding(new Insets(16, 32, 28, 32));
        scroll.viewportBoundsProperty().addListener((obs, o, bounds) ->
            content.setMinHeight(Math.max(0, bounds.getHeight())));

        context.tabsState().currentElementIdProperty().addListener((obs, o, v) -> refresh());
        refresh();
    }

    private void refresh() {
        Holder.Reference<Enchantment> selected = selectedEnchantment();
        if (selected == null) return;

        Section section = new Section("enchantment:section.global.description");

        ResponsiveGrid cardsGrid = buildCardsGrid(selected);
        section.addContent(cardsGrid, buildModeSelector());

        section.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (context.packState().hasSelectedPack()) return;
            e.consume();
            showPackRequiredDialog();
        });

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        content.getChildren().setAll(section, spacer, buildSupportCard());
    }

    private ResponsiveGrid buildCardsGrid(Holder.Reference<Enchantment> holder) {
        Enchantment e = holder.value();
        Identifier id = holder.key().identifier();

        ResponsiveGrid grid = new ResponsiveGrid(ResponsiveGrid.autoFit(256))
            .atMost(StudioBreakpoint.XL, ResponsiveGrid.fixed(1));

        Counter maxLevel = new Counter(1, 127, 1, e.getMaxLevel());
        maxLevel.valueProperty().addListener((obs, oldVal, newVal) ->
                context.gateway().apply(enchantmentAction(id, ench -> withDefinition(ench, d ->
                        new Enchantment.EnchantmentDefinition(d.supportedItems(), d.primaryItems(),
                                d.weight(), newVal.intValue(), d.minCost(), d.maxCost(), d.anvilCost(), d.slots())))));

        Counter weight = new Counter(1, 1024, 1, e.getWeight());
        weight.valueProperty().addListener((obs, oldVal, newVal) ->
                context.gateway().apply(enchantmentAction(id, ench -> withDefinition(ench, d ->
                        new Enchantment.EnchantmentDefinition(d.supportedItems(), d.primaryItems(),
                                newVal.intValue(), d.maxLevel(), d.minCost(), d.maxCost(), d.anvilCost(), d.slots())))));

        Counter anvilCost = new Counter(0, 255, 1, e.getAnvilCost());
        anvilCost.valueProperty().addListener((obs, oldVal, newVal) ->
                context.gateway().apply(enchantmentAction(id, ench -> withDefinition(ench, d ->
                        new Enchantment.EnchantmentDefinition(d.supportedItems(), d.primaryItems(),
                                d.weight(), d.maxLevel(), d.minCost(), d.maxCost(), newVal.intValue(), d.slots())))));

        grid.addItem(new TemplateCard(MAX_LEVEL_ICON, "enchantment:global.maxLevel.title",
                "enchantment:global.explanation.list.1", maxLevel));
        grid.addItem(new TemplateCard(WEIGHT_ICON, "enchantment:global.weight.title",
                "enchantment:global.explanation.list.2", weight));
        grid.addItem(new TemplateCard(ANVIL_COST_ICON, "enchantment:global.anvilCost.title",
                "enchantment:global.explanation.list.3", anvilCost));
        return grid;
    }

    private static EditorAction<Enchantment> enchantmentAction(Identifier id, UnaryOperator<Enchantment> transform) {
        return new EditorAction<>("enchantment", id, Enchantment.class, transform);
    }

    private static Enchantment withDefinition(Enchantment e, UnaryOperator<Enchantment.EnchantmentDefinition> transform) {
        return new Enchantment(e.description(), transform.apply(e.definition()), e.exclusiveSet(), e.effects());
    }

    private Selector buildModeSelector() {
        LinkedHashMap<String, String> modeOptions = new LinkedHashMap<>();
        modeOptions.put("normal", I18n.get("enchantment:global.mode.enum.normal"));
        modeOptions.put("soft_delete", I18n.get("enchantment:global.mode.enum.soft_delete"));
        modeOptions.put("only_creative", I18n.get("enchantment:global.mode.enum.only_creative"));

        return new Selector(
                "enchantment:global.mode.title",
                "enchantment:global.mode.description",
                modeOptions, "normal", v -> {
                });
    }

    private StackPane buildSupportCard() {
        SvgIcon logo = new SvgIcon(LOGO, 384, Color.WHITE);
        logo.setOpacity(0.2);
        logo.setManaged(false);
        logo.setMouseTransparent(true);

        Label title = new Label(I18n.get("supports.title"));
        title.getStyleClass().add("support-card-title");
        title.setFont(VoxelFonts.of(VoxelFonts.Variant.SEMI_BOLD, 30));
        title.setWrapText(true);

        Label desc = new Label(I18n.get("supports.description"));
        desc.getStyleClass().add("support-card-desc");
        desc.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 14));
        desc.setWrapText(true);
        VBox.setMargin(desc, new Insets(8, 0, 0, 0));

        VBox textBlock = new VBox(title, desc);

        Label advantagesHeading = new Label(I18n.get("supports.advantages"));
        advantagesHeading.getStyleClass().add("support-card-advantages-heading");
        advantagesHeading.setFont(VoxelFonts.of(VoxelFonts.Variant.BOLD, 20));
        VBox.setMargin(advantagesHeading, new Insets(24, 0, 16, 0));

        GridPane advantagesGrid = buildAdvantagesGrid();
        VBox advantagesSection = new VBox(advantagesHeading, advantagesGrid);
        HBox.setHgrow(advantagesSection, Priority.ALWAYS);

        Button donateBtn = new Button(Button.Variant.SHIMMER, Button.Size.LG,
                I18n.get("donate"));
        donateBtn.setOnAction(() -> BrowserUtils.openBrowser("https://streamelements.com/hardoudou/tip"));

        SvgIcon patreonIcon = new SvgIcon(PATREON_ICON, 16, Color.WHITE);
        Button patreonBtn = new Button(Button.Variant.PATREON, Button.Size.LG,
                I18n.get("supports.become"), patreonIcon);
        patreonBtn.setOnAction(() -> BrowserUtils.openBrowser("https://www.patreon.com/hardel"));
        donateBtn.setMaxWidth(Double.MAX_VALUE);
        patreonBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(donateBtn, Priority.ALWAYS);
        HBox.setHgrow(patreonBtn, Priority.ALWAYS);

        HBox buttonsRow = new HBox(16, donateBtn, patreonBtn);
        buttonsRow.setAlignment(Pos.CENTER_RIGHT);
        buttonsRow.setPadding(new Insets(32, 0, 0, 0));

        Region btnSpacer = new Region();
        VBox.setVgrow(btnSpacer, Priority.ALWAYS);
        VBox buttonsWrapper = new VBox(btnSpacer, buttonsRow);
        buttonsWrapper.setAlignment(Pos.BOTTOM_RIGHT);

        HBox bottom = new HBox(16, advantagesSection, buttonsWrapper);
        bottom.setPadding(new Insets(16, 0, 0, 0));

        VBox cardContent = new VBox(textBlock, bottom);
        cardContent.setPadding(new Insets(32, 32, 32, 48));
        StackPane.setAlignment(cardContent, Pos.TOP_LEFT);

        StackPane card = new StackPane();
        card.getStyleClass().add("support-card");
        card.setAlignment(Pos.TOP_LEFT);

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
        grid.setHgap(32);
        grid.setVgap(16);
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
        text.setFont(VoxelFonts.of(VoxelFonts.Variant.SEMI_BOLD, 14));
        HBox item = new HBox(8, check, text);
        item.setAlignment(Pos.CENTER_LEFT);
        return item;
    }

    private void showPackRequiredDialog() {
        Label message = new Label(I18n.get("studio:pack.required.message"));
        message.setTextFill(VoxelColors.ZINC_400);
        message.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 13));
        message.setWrapText(true);

        Dialog dialog = new Dialog("studio:pack.required.title", message);

        Button cancelBtn = new Button(Button.Variant.GHOST_BORDER, Button.Size.SM,
                I18n.get("studio:action.cancel"));
        cancelBtn.setOnAction(dialog::close);

        Button createBtn = new Button(Button.Variant.SHIMMER, Button.Size.SM,
                I18n.get("studio:pack.create"));
        createBtn.setOnAction(() -> {
            dialog.close();
            var createDialog = PackCreateDialog.create(context);
            createDialog.show(getScene().getWindow());
        });

        dialog.addFooterButton(cancelBtn).addFooterButton(createBtn);
        dialog.show(getScene().getWindow());
    }

    private Holder.Reference<Enchantment> selectedEnchantment() {
        String id = context.tabsState().currentElementId();
        var enchantments = context.enchantments();
        if (enchantments.isEmpty()) return null;
        if (id != null && !id.isBlank()) {
            for (var h : enchantments) {
                if (h.key().identifier().toString().equals(id)) return h;
            }
        }
        return enchantments.getFirst();
    }
}

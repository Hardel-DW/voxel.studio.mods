package fr.hardel.asset_editor.client.javafx.components.layout;

import fr.hardel.asset_editor.client.javafx.VoxelResourceLoader;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.components.ui.Button;
import fr.hardel.asset_editor.client.javafx.components.ui.SvgIcon;
import fr.hardel.asset_editor.client.javafx.lib.utils.BrowserUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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

public final class SupportCard extends StackPane {

    private static final Identifier LOGO = Identifier.fromNamespaceAndPath("asset_editor", "icons/logo.svg");
    private static final Identifier SHINE = Identifier.fromNamespaceAndPath("asset_editor",
        "textures/shine.png");
    private static final Identifier CHECK = Identifier.fromNamespaceAndPath("asset_editor", "icons/check.svg");
    private static final Identifier PATREON_ICON = Identifier.fromNamespaceAndPath("asset_editor",
        "icons/company/patreon.svg");
    private static final String[] ADVANTAGES = { "early_access", "submit_ideas", "discord_role", "live_voxel" };

    public SupportCard() {
        getStyleClass().add("layout-support-card");
        setAlignment(Pos.TOP_LEFT);

        SvgIcon logo = new SvgIcon(LOGO, 384, Color.WHITE);
        logo.setOpacity(0.2);
        logo.setManaged(false);
        logo.setMouseTransparent(true);

        Label title = new Label(I18n.get("supports:title"));
        title.getStyleClass().add("layout-support-card-title");
        title.setFont(VoxelFonts.of(VoxelFonts.Variant.SEMI_BOLD, 30));
        title.setWrapText(true);

        Label desc = new Label(I18n.get("supports:description"));
        desc.getStyleClass().add("layout-support-card-desc");
        desc.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 14));
        desc.setWrapText(true);
        VBox.setMargin(desc, new Insets(8, 0, 0, 0));

        VBox textBlock = new VBox(title, desc);

        Label advantagesHeading = new Label(I18n.get("supports:advantages"));
        advantagesHeading.getStyleClass().add("layout-support-card-advantages-heading");
        advantagesHeading.setFont(VoxelFonts.of(VoxelFonts.Variant.BOLD, 20));
        VBox.setMargin(advantagesHeading, new Insets(24, 0, 16, 0));

        GridPane advantagesGrid = buildAdvantagesGrid();
        VBox advantagesSection = new VBox(advantagesHeading, advantagesGrid);
        HBox.setHgrow(advantagesSection, Priority.ALWAYS);

        Button donateBtn = new Button(Button.Variant.SHIMMER, Button.Size.LG, I18n.get("supports:donate"));
        donateBtn.setOnAction(() -> BrowserUtils.openBrowser("https://streamelements.com/hardoudou/tip"));

        SvgIcon patreonIcon = new SvgIcon(PATREON_ICON, 16, Color.WHITE);
        Button patreonBtn = new Button(Button.Variant.PATREON, Button.Size.LG, I18n.get("supports:become"),
            patreonIcon);
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

        try (var stream = VoxelResourceLoader.open(SHINE)) {
            ImageView shine = new ImageView(new Image(stream));
            ColorAdjust shineEffect = new ColorAdjust();
            shineEffect.setHue(0.5);
            shineEffect.setBrightness(-0.8);
            shine.setEffect(shineEffect);
            shine.setPreserveRatio(false);
            shine.setManaged(false);
            shine.setMouseTransparent(true);
            shine.fitWidthProperty().bind(widthProperty());
            shine.fitHeightProperty().bind(heightProperty());
            getChildren().add(shine);
        } catch (Exception ignored) {
        }

        logo.layoutXProperty().bind(widthProperty().subtract(288));
        logo.setLayoutY(-96);
        getChildren().addAll(logo, cardContent);

        Rectangle clip = new Rectangle();
        clip.setArcWidth(32);
        clip.setArcHeight(32);
        widthProperty().addListener((obs, o, w) -> clip.setWidth(w.doubleValue()));
        heightProperty().addListener((obs, o, h) -> clip.setHeight(h.doubleValue()));
        setClip(clip);
    }

    private static GridPane buildAdvantagesGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(32);
        grid.setVgap(16);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(c);
        }
        for (int i = 0; i < ADVANTAGES.length; i++) {
            grid.add(buildAdvantageItem("supports:advantages." + ADVANTAGES[i]), i % 2, i / 2);
        }
        return grid;
    }

    private static HBox buildAdvantageItem(String key) {
        SvgIcon check = new SvgIcon(CHECK, 16, Color.WHITE);
        Label text = new Label(I18n.get(key));
        text.getStyleClass().add("layout-support-card-benefit");
        text.setFont(VoxelFonts.of(VoxelFonts.Variant.SEMI_BOLD, 14));
        HBox item = new HBox(8, check, text);
        item.setAlignment(Pos.CENTER_LEFT);
        return item;
    }
}

package fr.hardel.asset_editor.client.javafx.components.page.enchantment;

import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.components.ui.ResourceImageIcon;
import fr.hardel.asset_editor.client.javafx.components.ui.SimpleCard;
import fr.hardel.asset_editor.client.javafx.components.ui.ToggleSwitch;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;

public final class EnchantmentOverviewCard extends SimpleCard {

    private record OverviewCase(String titleKey, Identifier image) {
    }

    private static final List<OverviewCase> DEFAULT_CASES = List.of(
            new OverviewCase("enchantment:overview.enchanting_table", feature("block/enchanting_table")),
            new OverviewCase("enchantment:overview.chest", feature("block/chest")),
            new OverviewCase("enchantment:overview.tradeable", feature("item/enchanted_book")),
            new OverviewCase("enchantment:overview.tradeable_equipment", feature("item/enchanted_item")));

    public EnchantmentOverviewCard(Holder.Reference<Enchantment> holder, Runnable onOpen) {
        super(new Insets(16));
        getStyleClass().add("enchantment-card");
        setMaxWidth(Double.MAX_VALUE);
        setOnMouseClicked(e -> onOpen.run());

        visualCard.getStyleClass().add("enchantment-card-surface");
        installOverviewHover();

        String displayName = holder.value().description().getString();
        int maxLevel = holder.value().getMaxLevel();
        boolean vanilla = "minecraft".equals(holder.key().identifier().getNamespace());

        HBox top = buildTopRow(displayName, maxLevel, null, vanilla);
        FlowPane cases = buildCases();

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Region divider = new Region();
        divider.getStyleClass().add("enchantment-card-divider");
        divider.setMaxWidth(Double.MAX_VALUE);

        Label configure = new Label(I18n.get("configure"));
        configure.getStyleClass().add("enchantment-card-configure");
        configure.setMaxWidth(Double.MAX_VALUE);

        VBox configureWrap = new VBox(configure);
        configureWrap.setPadding(new Insets(16, 0, 0, 0));

        contentBox.getChildren().setAll(top, cases, spacer, divider, configureWrap);
    }

    private HBox buildTopRow(String displayName, int maxLevel, Identifier previewTexture, boolean vanilla) {
        StackPane iconWrap = new StackPane();
        iconWrap.getStyleClass().add("enchantment-card-icon");
        iconWrap.setMinSize(40, 40);
        iconWrap.setPrefSize(40, 40);
        iconWrap.setMaxSize(40, 40);

        if (previewTexture != null) {
            iconWrap.getChildren().add(new ResourceImageIcon(previewTexture, 40));
        }
        if (iconWrap.getChildren().isEmpty()) {
            Label fallback = new Label("?");
            fallback.getStyleClass().add("enchantment-card-placeholder");
            iconWrap.getChildren().add(fallback);
        }

        Label name = new Label(displayName);
        name.getStyleClass().add("enchantment-card-resource-name");
        name.setMaxWidth(Double.MAX_VALUE);

        Label level = new Label(I18n.get("enchantment:overview.level") + " " + maxLevel);
        level.getStyleClass().add("enchantment-card-level");
        level.setFont(VoxelFonts.of(VoxelFonts.Variant.MINECRAFT_TEN, 10));

        VBox info = new VBox(2, name, level);
        info.setMinWidth(0);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox left = new HBox(12, iconWrap, info);
        left.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(left, Priority.ALWAYS);

        HBox top = new HBox(left);
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(0, 0, 12, 0));

        if (!vanilla) {
            ToggleSwitch stateSwitch = new ToggleSwitch();
            stateSwitch.setValue(true);
            stateSwitch.addEventFilter(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);
            top.getChildren().add(stateSwitch);
        }

        return top;
    }

    private FlowPane buildCases() {
        FlowPane cases = new FlowPane();
        cases.setHgap(8);
        cases.setVgap(8);
        cases.setPadding(new Insets(0, 0, 16, 0));

        for (OverviewCase cardCase : DEFAULT_CASES) {
            StackPane caseNode = new StackPane();
            caseNode.getStyleClass().add("enchantment-card-case");
            caseNode.setMinSize(16, 16);
            caseNode.setPrefSize(16, 16);
            caseNode.setMaxSize(16, 16);
            caseNode.getChildren().add(new ResourceImageIcon(cardCase.image(), 16));
            Tooltip.install(caseNode, new Tooltip(I18n.get(cardCase.titleKey())));
            caseNode.addEventFilter(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);
            cases.getChildren().add(caseNode);
        }

        return cases;
    }

    private void installOverviewHover() {
        TranslateTransition hoverIn = new TranslateTransition(Duration.millis(150), visualCard);
        hoverIn.setToY(-2);
        TranslateTransition hoverOut = new TranslateTransition(Duration.millis(150), visualCard);
        hoverOut.setToY(0);
        setOnMouseEntered(e -> hoverIn.playFromStart());
        setOnMouseExited(e -> hoverOut.playFromStart());
    }

    private static Identifier feature(String path) {
        return Identifier.fromNamespaceAndPath("asset_editor", "textures/features/" + path + ".png");
    }
}

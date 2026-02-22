package fr.hardel.asset_editor.client.javafx.components.page.enchantment;

import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.lib.data.mock.StudioMockEnchantment;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import net.minecraft.client.resources.language.I18n;

/**
 * Grid card matching EnchantmentCard.tsx:
 * icon (40px) + name/level → tags placeholder → spacer → divider → configure.
 */
public final class EnchantmentOverviewCard extends VBox {

    public EnchantmentOverviewCard(StudioMockEnchantment enchantment, Runnable onOpen) {
        getStyleClass().add("enchantment-card");
        setPadding(new Insets(16));
        setCursor(Cursor.HAND);
        setOnMouseClicked(e -> onOpen.run());

        Region icon = new Region();
        icon.getStyleClass().add("enchantment-card-icon");
        icon.setPrefSize(40, 40);
        icon.setMinSize(40, 40);
        icon.setMaxSize(40, 40);

        Label name = new Label(enchantment.resource());
        name.getStyleClass().add("enchantment-card-resource-name");
        name.setMaxWidth(Double.MAX_VALUE);

        Label level = new Label(I18n.get("enchantment:overview.level") + " " + enchantment.maxLevel());
        level.getStyleClass().add("enchantment-card-level");
        level.setFont(VoxelFonts.minecraft(VoxelFonts.Minecraft.TEN, 10));

        VBox info = new VBox(2, name, level);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox top = new HBox(12, icon, info);
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(0, 0, 12, 0));

        // Tags placeholder — populated once registry data is available
        HBox tags = new HBox(8);
        tags.setPadding(new Insets(0, 0, 16, 0));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Region divider = new Region();
        divider.getStyleClass().add("enchantment-card-divider");
        divider.setMaxWidth(Double.MAX_VALUE);

        Label configure = new Label(I18n.get("configure"));
        configure.getStyleClass().add("enchantment-card-configure");
        configure.setMaxWidth(Double.MAX_VALUE);

        getChildren().addAll(top, tags, spacer, divider, configure);
    }
}




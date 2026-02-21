package fr.hardel.asset_editor.client.javafx.editor.page.enchantment;

import fr.hardel.asset_editor.client.javafx.editor.model.StudioMockEnchantment;
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
 * List row matching EnchantmentOverviewList.tsx:
 * icon (32px) + name / (identifier • level) → divider → configure.
 */
public final class EnchantmentOverviewRow extends HBox {

    public EnchantmentOverviewRow(StudioMockEnchantment enchantment, Runnable onOpen) {
        getStyleClass().add("enchantment-row");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(16);
        setPadding(new Insets(12));
        setCursor(Cursor.HAND);
        setOnMouseClicked(e -> onOpen.run());

        Region icon = new Region();
        icon.getStyleClass().add("enchantment-row-icon");
        icon.setPrefSize(32, 32);
        icon.setMinSize(32, 32);
        icon.setMaxSize(32, 32);

        Label name = new Label(enchantment.resource());
        name.getStyleClass().add("enchantment-row-name");

        Label identifier = new Label(enchantment.uniqueKey());
        identifier.getStyleClass().add("enchantment-row-identifier");

        Label bullet = new Label("•");
        bullet.getStyleClass().add("enchantment-row-separator");

        Label level = new Label(I18n.get("enchantment:overview.level") + " " + enchantment.maxLevel());
        level.getStyleClass().add("enchantment-row-level");

        HBox subInfo = new HBox(8, identifier, bullet, level);
        subInfo.setAlignment(Pos.CENTER_LEFT);

        VBox info = new VBox(2, name, subInfo);
        HBox.setHgrow(info, Priority.ALWAYS);

        Region divider = new Region();
        divider.getStyleClass().add("enchantment-row-divider");
        divider.setPrefSize(1, 16);
        divider.setMinSize(1, 16);
        divider.setMaxSize(1, 16);

        Label configure = new Label(I18n.get("configure"));
        configure.getStyleClass().add("enchantment-row-configure");

        getChildren().addAll(icon, info, divider, configure);
    }
}

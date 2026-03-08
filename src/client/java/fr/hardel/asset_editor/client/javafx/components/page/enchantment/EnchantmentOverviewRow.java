package fr.hardel.asset_editor.client.javafx.components.page.enchantment;

import fr.hardel.asset_editor.client.javafx.components.ui.ResourceImageIcon;
import fr.hardel.asset_editor.client.javafx.components.ui.ToggleSwitch;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.action.EnchantmentMutations;
import fr.hardel.asset_editor.client.javafx.lib.store.RegistryElementStore.ElementEntry;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantment;

public final class EnchantmentOverviewRow extends HBox {

    public EnchantmentOverviewRow(StudioContext context, ElementEntry<Enchantment> entry, Runnable onOpen) {
        getStyleClass().add("enchantment-row");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(16);
        setPadding(new Insets(12));
        setCursor(Cursor.HAND);
        setOnMouseClicked(e -> onOpen.run());

        StackPane iconWrap = new StackPane();
        iconWrap.getStyleClass().add("enchantment-row-icon");
        iconWrap.setMinSize(32, 32);
        iconWrap.setPrefSize(32, 32);
        iconWrap.setMaxSize(32, 32);

        var previewTexture = EnchantmentMutations.previewTexture(entry.data());
        if (previewTexture != null) {
            iconWrap.getChildren().add(new ResourceImageIcon(previewTexture, 32));
        }
        if (iconWrap.getChildren().isEmpty()) {
            Label fallback = new Label("?");
            fallback.getStyleClass().add("enchantment-row-placeholder");
            iconWrap.getChildren().add(fallback);
        }

        Label name = new Label(entry.data().description().getString());
        name.getStyleClass().add("enchantment-row-name");

        Label identifier = new Label(entry.id().toString());
        identifier.getStyleClass().add("enchantment-row-identifier");

        Label bullet = new Label("\u2022");
        bullet.getStyleClass().add("enchantment-row-separator");

        Label level = new Label(I18n.get("enchantment:overview.level") + " " + entry.data().getMaxLevel());
        level.getStyleClass().add("enchantment-row-level");

        HBox subInfo = new HBox(8, identifier, bullet, level);
        subInfo.setAlignment(Pos.CENTER_LEFT);

        VBox info = new VBox(2, name, subInfo);
        info.setMinWidth(0);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox left = new HBox(16, iconWrap, info);
        left.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(left, Priority.ALWAYS);

        HBox right = new HBox(8);
        right.setAlignment(Pos.CENTER_RIGHT);

        ToggleSwitch stateSwitch = new ToggleSwitch();
        stateSwitch.setValue(!EnchantmentMutations.isSoftDeleted(entry));
        stateSwitch.addEventFilter(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);
        stateSwitch.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (oldValue == null || newValue == null || oldValue.equals(newValue)) return;

            var current = context.elementStore().get(Registries.ENCHANTMENT, entry.id());
            boolean enabled = current == null || !EnchantmentMutations.isSoftDeleted(current);
            if (Boolean.valueOf(newValue).equals(enabled)) return;

            var result = context.gateway().applyCustom(Registries.ENCHANTMENT, entry.id(), EnchantmentMutations.toggleDisabled());
            if (!result.isApplied()) {
                stateSwitch.setValue(enabled);
            }
        });
        right.getChildren().add(stateSwitch);

        Region divider = new Region();
        divider.getStyleClass().add("enchantment-row-divider");
        divider.setMinSize(1, 16);
        divider.setPrefSize(1, 16);
        divider.setMaxSize(1, 16);
        HBox.setMargin(divider, new Insets(0, 8, 0, 8));

        Label configure = new Label(I18n.get("configure"));
        configure.getStyleClass().add("enchantment-row-configure");

        right.getChildren().addAll(divider, configure);
        getChildren().addAll(left, right);
    }
}

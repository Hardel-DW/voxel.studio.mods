package fr.hardel.asset_editor.client.javafx.components.layout.editor;

import fr.hardel.asset_editor.client.javafx.context.StudioContext;
import fr.hardel.asset_editor.client.javafx.data.StudioConcept;
import fr.hardel.asset_editor.client.javafx.data.StudioTabDefinition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import net.minecraft.client.resources.language.I18n;

public final class EnchantmentHeaderTabs extends HBox {

    private final StudioContext context;

    public EnchantmentHeaderTabs(StudioContext context) {
        this.context = context;
        getStyleClass().add("enchantment-header-tabs");
        setSpacing(4);
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(8, 0, 0, 0));
        context.router().routeProperty().addListener((obs, oldValue, newValue) -> refresh());
        refresh();
    }

    private void refresh() {
        getChildren().clear();
        for (StudioTabDefinition tab : StudioConcept.ENCHANTMENT.tabs())
            getChildren().add(tabButton(tab, context.router().currentRoute() == tab.route()));
    }

    private Button tabButton(StudioTabDefinition tab, boolean active) {
        Button button = new Button(I18n.get(tab.translationKey()));
        button.getStyleClass().add("enchantment-header-tab-button");
        if (active)
            button.getStyleClass().add("enchantment-header-tab-button-active");
        button.setOnAction(e -> context.router().navigate(tab.route()));
        return button;
    }
}




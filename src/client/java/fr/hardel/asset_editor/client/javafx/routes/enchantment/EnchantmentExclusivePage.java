package fr.hardel.asset_editor.client.javafx.routes.enchantment;

import fr.hardel.asset_editor.client.javafx.components.page.enchantment.ExclusiveGroupSection;
import fr.hardel.asset_editor.client.javafx.components.page.enchantment.ExclusiveSingleSection;
import fr.hardel.asset_editor.client.javafx.components.ui.SectionSelector;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import net.minecraft.client.resources.language.I18n;

import java.util.LinkedHashMap;

public final class EnchantmentExclusivePage extends VBox {

    private final StudioContext context;
    private final VBox content = new VBox(32);
    private SectionSelector selector;
    private VBox groupSection;
    private VBox singleSection;
    private String currentMode = "group";

    public EnchantmentExclusivePage(StudioContext context) {
        this.context = context;
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("enchantment-subpage-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().add(scroll);

        content.setPadding(new Insets(32));
        refresh();
    }

    private void refresh() {
        LinkedHashMap<String, String> tabs = new LinkedHashMap<>();
        tabs.put("group",  I18n.get("enchantment:toggle.group.title"));
        tabs.put("single", I18n.get("enchantment:toggle.individual.title"));

        selector = new SectionSelector(
            "enchantment:section.exclusive.description",
            tabs,
            currentMode,
            this::onModeChange
        );
        groupSection = new ExclusiveGroupSection();
        singleSection = new ExclusiveSingleSection(context);
        content.getChildren().setAll(selector);
        selector.setContent(groupSection);
    }

    private void onModeChange(String mode) {
        currentMode = mode;
        selector.setContent("single".equals(mode) ? singleSection : groupSection);
    }
}

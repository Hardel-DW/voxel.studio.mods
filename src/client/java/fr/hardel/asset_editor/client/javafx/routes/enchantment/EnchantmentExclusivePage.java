package fr.hardel.asset_editor.client.javafx.routes.enchantment;

import fr.hardel.asset_editor.client.javafx.components.page.enchantment.ExclusiveGroupSection;
import fr.hardel.asset_editor.client.javafx.components.page.enchantment.ExclusiveSingleSection;
import fr.hardel.asset_editor.client.javafx.components.ui.SectionSelector;
import fr.hardel.asset_editor.client.javafx.lib.RegistryPage;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.LinkedHashMap;

public final class EnchantmentExclusivePage extends RegistryPage<Enchantment> {

    private SectionSelector selector;
    private String currentMode = "group";

    public EnchantmentExclusivePage(StudioContext context) {
        super(context, Registries.ENCHANTMENT, "enchantment-subpage-scroll", 32, new Insets(32));
    }

    @Override
    protected void buildContent() {
        LinkedHashMap<String, String> tabs = new LinkedHashMap<>();
        tabs.put("group", I18n.get("enchantment:toggle.group.title"));
        tabs.put("single", I18n.get("enchantment:toggle.individual.title"));

        selector = new SectionSelector(
            "enchantment:section.exclusive.description",
            tabs,
            currentMode,
            this::onModeChange
        );

        content().getChildren().setAll(selector);

        var exclusiveSelector = select(entry -> entry.data().exclusiveSet().unwrapKey()
                .map(k -> k.location().toString()).orElse(""));

        VBox groupSection = new ExclusiveGroupSection(context(), currentId(), exclusiveSelector);
        VBox singleSection = new ExclusiveSingleSection(context(), currentId());

        selector.setContent("single".equals(currentMode) ? singleSection : groupSection);

        onModeChangeInternal = mode -> {
            currentMode = mode;
            selector.setContent("single".equals(mode) ? singleSection : groupSection);
        };
    }

    private java.util.function.Consumer<String> onModeChangeInternal;

    private void onModeChange(String mode) {
        if (onModeChangeInternal != null) onModeChangeInternal.accept(mode);
        else currentMode = mode;
    }
}

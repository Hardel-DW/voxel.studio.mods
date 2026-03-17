package fr.hardel.asset_editor.client.javafx.routes.enchantment;

import fr.hardel.asset_editor.client.javafx.components.page.enchantment.ExclusiveGroupSection;
import fr.hardel.asset_editor.client.javafx.components.page.enchantment.ExclusiveSingleSection;
import fr.hardel.asset_editor.client.javafx.components.ui.SectionSelector;
import fr.hardel.asset_editor.client.javafx.lib.RegistryPage;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.network.EditorAction;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class EnchantmentExclusivePage extends RegistryPage<Enchantment> {

    private SectionSelector selector;
    private VBox groupSection;
    private VBox singleSection;
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
            I18n.get("enchantment:section.exclusive.description"),
            tabs,
            currentMode,
            this::onModeChange
        );

        content().getChildren().setAll(selector);

        groupSection = new ExclusiveGroupSection(
                context(), currentId(),
                (mutation, action) -> applyAction(mutation, action).isApplied(),
                tagId -> applyTagToggle(tagId).isApplied());

        var directExclusiveSelector = select(entry -> {
            if (entry.data().exclusiveSet().unwrapKey().isPresent()) {
                return Set.<String>of();
            }
            return entry.data().exclusiveSet().stream()
                    .map(holder -> holder.unwrapKey()
                            .map(k -> k.identifier().toString())
                            .orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        });

        singleSection = new ExclusiveSingleSection(
                context(),
                directExclusiveSelector,
                (mutation, action) -> applyAction(mutation, action).isApplied());

        selector.setContent("single".equals(currentMode) ? singleSection : groupSection);
    }

    private void onModeChange(String mode) {
        currentMode = mode;
        if (selector != null) {
            selector.setContent("single".equals(mode) ? singleSection : groupSection);
        }
    }
}

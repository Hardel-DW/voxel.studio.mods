package fr.hardel.asset_editor.client.javafx.data;

import fr.hardel.asset_editor.client.javafx.routes.StudioRoute;
import net.minecraft.resources.Identifier;

import java.util.List;

public enum StudioConcept {
    ENCHANTMENT(
            "studio.concept.enchantment",
            Identifier.fromNamespaceAndPath("asset_editor", "textures/studio/concept/enchantment.png"),
            List.of(
                    new StudioTabDefinition("global", "enchantment:section.global", StudioRoute.ENCHANTMENT_MAIN),
                    new StudioTabDefinition("find", "enchantment:section.find", StudioRoute.ENCHANTMENT_FIND),
                    new StudioTabDefinition("slots", "enchantment:section.slots", StudioRoute.ENCHANTMENT_SLOTS),
                    new StudioTabDefinition("items", "enchantment:section.supported", StudioRoute.ENCHANTMENT_ITEMS),
                    new StudioTabDefinition("exclusive", "enchantment:section.exclusive", StudioRoute.ENCHANTMENT_EXCLUSIVE),
                    new StudioTabDefinition("technical", "enchantment:section.technical", StudioRoute.ENCHANTMENT_TECHNICAL)
            )),
    LOOT_TABLE(
            "studio.concept.loot_table",
            Identifier.fromNamespaceAndPath("asset_editor", "textures/studio/concept/loot_table.png"),
            List.of()),
    RECIPE(
            "studio.concept.recipe",
            Identifier.fromNamespaceAndPath("asset_editor", "textures/studio/concept/recipe.png"),
            List.of()),
    STRUCTURE(
            "studio.concept.structure",
            Identifier.fromNamespaceAndPath("asset_editor", "textures/studio/concept/structure.png"),
            List.of());

    private final String titleKey;
    private final Identifier icon;
    private final List<StudioTabDefinition> tabs;

    StudioConcept(String titleKey, Identifier icon, List<StudioTabDefinition> tabs) {
        this.titleKey = titleKey;
        this.icon = icon;
        this.tabs = tabs;
    }

    public String titleKey() {
        return titleKey;
    }

    public Identifier icon() {
        return icon;
    }

    public List<StudioTabDefinition> tabs() {
        return tabs;
    }
}



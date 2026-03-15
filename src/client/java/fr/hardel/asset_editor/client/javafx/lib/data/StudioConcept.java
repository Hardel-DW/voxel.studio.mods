package fr.hardel.asset_editor.client.javafx.lib.data;

import fr.hardel.asset_editor.client.javafx.routes.StudioRoute;
import fr.hardel.asset_editor.permission.StudioPermissions;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.List;
import java.util.Optional;

public enum StudioConcept {
    ENCHANTMENT(
        Registries.ENCHANTMENT,
        "studio.concept.enchantment",
        StudioRoute.ENCHANTMENT_OVERVIEW,
        Identifier.fromNamespaceAndPath("minecraft", "textures/studio/concept/enchantment.png"),
        List.of(
            new StudioTabDefinition("global", "enchantment:section.global", StudioRoute.ENCHANTMENT_MAIN),
            new StudioTabDefinition("find", "enchantment:section.find", StudioRoute.ENCHANTMENT_FIND),
            new StudioTabDefinition("slots", "enchantment:section.slots", StudioRoute.ENCHANTMENT_SLOTS),
            new StudioTabDefinition("items", "enchantment:section.supported", StudioRoute.ENCHANTMENT_ITEMS),
            new StudioTabDefinition("exclusive", "enchantment:section.exclusive", StudioRoute.ENCHANTMENT_EXCLUSIVE),
            new StudioTabDefinition("technical", "enchantment:section.technical", StudioRoute.ENCHANTMENT_TECHNICAL))),
    LOOT_TABLE(
        Registries.LOOT_TABLE,
        "studio.concept.loot_table",
        StudioRoute.LOOT_TABLE_OVERVIEW,
        Identifier.fromNamespaceAndPath("minecraft", "textures/studio/concept/loot_table.png"),
        List.of(
            new StudioTabDefinition("main", "loot:section.main", StudioRoute.LOOT_TABLE_MAIN),
            new StudioTabDefinition("pools", "loot:section.pools", StudioRoute.LOOT_TABLE_POOLS))),
    RECIPE(
        Registries.RECIPE,
        "studio.concept.recipe",
        StudioRoute.RECIPE_OVERVIEW,
        Identifier.fromNamespaceAndPath("minecraft", "textures/studio/concept/recipe.png"),
        List.of(
            new StudioTabDefinition("main", "recipe:section.main", StudioRoute.RECIPE_MAIN))),
    STRUCTURE(
        Registries.STRUCTURE,
        "studio.concept.structure",
        StudioRoute.ENCHANTMENT_OVERVIEW,
        Identifier.fromNamespaceAndPath("minecraft", "textures/studio/concept/structure.png"),
        List.of());

    private final ResourceKey<? extends Registry<?>> registryKey;
    private final String titleKey;
    private final StudioRoute overviewRoute;
    private final Identifier icon;
    private final List<StudioTabDefinition> tabs;

    StudioConcept(ResourceKey<? extends Registry<?>> registryKey, String titleKey, StudioRoute overviewRoute, Identifier icon, List<StudioTabDefinition> tabs) {
        this.registryKey = registryKey;
        this.titleKey = titleKey;
        this.overviewRoute = overviewRoute;
        this.icon = icon;
        this.tabs = tabs;
    }

    public ResourceKey<? extends Registry<?>> registryKey() {
        return registryKey;
    }

    public String registry() {
        return registryKey.identifier().getPath();
    }

    public String titleKey() {
        return titleKey;
    }

    public StudioRoute overviewRoute() {
        return overviewRoute;
    }

    public Identifier icon() {
        return icon;
    }

    public List<StudioTabDefinition> tabs() {
        return tabs;
    }

    public List<StudioRoute> tabRoutes() {
        return tabs.stream().map(StudioTabDefinition::route).toList();
    }

    public static StudioConcept byRegistry(String registry) {
        for (StudioConcept concept : values()) {
            if (concept.registry().equals(registry))
                return concept;
        }
        return ENCHANTMENT;
    }

    public static StudioConcept byRoute(StudioRoute route) {
        return byRegistry(route.concept());
    }

    public static Optional<StudioConcept> firstAccessible(StudioPermissions permissions) {
        for (StudioConcept concept : values()) {
            if (concept == STRUCTURE) continue;
            if (permissions.canAccessRegistry(concept.registryKey())) return Optional.of(concept);
        }
        return Optional.empty();
    }
}

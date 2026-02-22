package fr.hardel.asset_editor.client.javafx.routes;

public enum StudioRoute {
    ENCHANTMENT_OVERVIEW("enchantment", true, false),
    ENCHANTMENT_MAIN("enchantment", false, true),
    ENCHANTMENT_FIND("enchantment", false, true),
    ENCHANTMENT_SLOTS("enchantment", false, true),
    ENCHANTMENT_ITEMS("enchantment", false, true),
    ENCHANTMENT_EXCLUSIVE("enchantment", false, true),
    ENCHANTMENT_TECHNICAL("enchantment", false, true),
    ENCHANTMENT_SIMULATION("enchantment", false, false),
    ENCHANTMENT_DNT("enchantment", false, false),
    ENCHANTMENT_YGGDRASIL("enchantment", false, false),
    LOOT_TABLE_OVERVIEW("loot_table", true, false),
    LOOT_TABLE_MAIN("loot_table", false, true),
    LOOT_TABLE_POOLS("loot_table", false, true),
    RECIPE_OVERVIEW("recipe", true, false),
    RECIPE_MAIN("recipe", false, true),
    CHANGES_MAIN("changes", false, false);

    private final String concept;
    private final boolean overview;
    private final boolean tabRoute;

    StudioRoute(String concept, boolean overview, boolean tabRoute) {
        this.concept = concept;
        this.overview = overview;
        this.tabRoute = tabRoute;
    }

    public String concept() {
        return concept;
    }

    public boolean isOverview() {
        return overview;
    }

    public boolean isTabRoute() {
        return tabRoute;
    }

    public static StudioRoute overviewOf(String concept) {
        return switch (concept) {
            case "loot_table" -> LOOT_TABLE_OVERVIEW;
            case "recipe" -> RECIPE_OVERVIEW;
            default -> ENCHANTMENT_OVERVIEW;
        };
    }
}



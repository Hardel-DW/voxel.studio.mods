package fr.hardel.asset_editor.client.javafx.routes;

public enum StudioRoute {
    ENCHANTMENT_OVERVIEW(true),
    ENCHANTMENT_MAIN(false),
    ENCHANTMENT_FIND(false),
    ENCHANTMENT_SLOTS(false),
    ENCHANTMENT_ITEMS(false),
    ENCHANTMENT_EXCLUSIVE(false),
    ENCHANTMENT_TECHNICAL(false),
    ENCHANTMENT_SIMULATION(false),
    ENCHANTMENT_DNT(false),
    ENCHANTMENT_YGGDRASIL(false);

    private final boolean overview;

    StudioRoute(boolean overview) {
        this.overview = overview;
    }

    public boolean isOverview() {
        return overview;
    }
}



package fr.hardel.asset_editor.client.javafx.lib.data;

public enum StudioSidebarView {
    SLOTS("enchantment:overview.sidebar.slots"),
    ITEMS("enchantment:overview.sidebar.items"),
    EXCLUSIVE("enchantment:overview.sidebar.exclusive");

    private final String translationKey;

    StudioSidebarView(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }
}



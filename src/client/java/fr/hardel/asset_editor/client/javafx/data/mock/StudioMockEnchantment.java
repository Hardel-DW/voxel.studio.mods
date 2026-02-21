package fr.hardel.asset_editor.client.javafx.data.mock;

import java.util.List;

public record StudioMockEnchantment(
        String namespace,
        String resource,
        int maxLevel,
        int weight,
        int anvilCost,
        List<String> slots,
        List<String> items,
        String exclusiveGroup) {

    public String uniqueKey() {
        return namespace + ":" + resource;
    }
}



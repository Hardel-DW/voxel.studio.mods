package fr.hardel.asset_editor.permission;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record ConceptRegistry(String name, List<String> dataFolders) {

    public static final ConceptRegistry ENCHANTMENT = new ConceptRegistry("enchantment",
            List.of("enchantment", "tags/enchantment"));
    public static final ConceptRegistry LOOT_TABLE = new ConceptRegistry("loot_table",
            List.of("loot_table", "tags/loot_table"));
    public static final ConceptRegistry RECIPE = new ConceptRegistry("recipe",
            List.of("recipe", "tags/recipe"));

    private static final Map<String, ConceptRegistry> BY_NAME = List.of(ENCHANTMENT, LOOT_TABLE, RECIPE)
            .stream().collect(Collectors.toUnmodifiableMap(ConceptRegistry::name, c -> c));

    public static ConceptRegistry byName(String name) {
        return BY_NAME.get(name);
    }

    public static Collection<ConceptRegistry> all() {
        return BY_NAME.values();
    }

    public static List<String> allDataFolders() {
        return all().stream().flatMap(c -> c.dataFolders().stream()).distinct().toList();
    }
}

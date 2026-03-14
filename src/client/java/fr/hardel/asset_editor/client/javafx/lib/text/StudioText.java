package fr.hardel.asset_editor.client.javafx.lib.text;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.Locale;
import java.util.Set;

public final class StudioText {

    public enum Domain {
        SLOT("slot"),
        ENCHANTMENT_SUPPORTED("enchantment.supported"),
        ENCHANTMENT_EXCLUSIVE("enchantment.exclusive"),
        EFFECT("effect");

        private final String key;

        Domain(String key) { this.key = key; }

        public String key() { return key; }
    }

    public static String resolve(ResourceKey<? extends Registry<?>> registry, Identifier id) {
        if (Registries.ITEM.equals(registry) && BuiltInRegistries.ITEM.containsKey(id)) {
            return BuiltInRegistries.ITEM.getValue(id).getName().getString();
        }

        String domain = registryDomain(registry);
        String key = domain + ":" + id.getNamespace() + "." + id.getPath();
        if (I18n.exists(key)) return I18n.get(key);

        String mcKey = id.toLanguageKey(registry.identifier().getPath());
        if (I18n.exists(mcKey)) return I18n.get(mcKey);

        return humanize(id.getPath());
    }

    public static String resolve(Domain domain, String id) {
        String key = domain.key() + ":" + id;
        return I18n.exists(key) ? I18n.get(key) : humanize(id);
    }

    private static String humanize(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String clean = raw.contains(":") ? raw.substring(raw.indexOf(':') + 1) : raw;
        String leaf = clean.contains("/") ? clean.substring(clean.lastIndexOf('/') + 1) : clean;
        String[] parts = leaf.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            builder.append(part.substring(1));
        }
        return builder.toString();
    }

    private static final Set<String> EFFECT_REGISTRIES = Set.of(
            "enchantment_effect_component_type",
            "enchantment_entity_effect_type",
            "enchantment_level_based_value_type",
            "enchantment_location_based_effect_type"
    );

    private static String registryDomain(ResourceKey<? extends Registry<?>> registry) {
        String path = registry.identifier().getPath();
        return EFFECT_REGISTRIES.contains(path) ? Domain.EFFECT.key() : path;
    }

    private StudioText() {}
}

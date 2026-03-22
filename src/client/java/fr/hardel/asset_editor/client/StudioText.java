package fr.hardel.asset_editor.client;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import java.util.Locale;

public final class StudioText {

    public static String resolve(String domain, Identifier id) {
        if ("item".equals(domain) && BuiltInRegistries.ITEM.containsKey(id)) {
            return BuiltInRegistries.ITEM.getValue(id).getName().getString();
        }

        String key = domain + ":" + id.toString();
        if (I18n.exists(key))
            return I18n.get(key);

        String mcKey = domain + "." + id.getNamespace() + "." + id.getPath();
        if (I18n.exists(mcKey))
            return I18n.get(mcKey);

        return humanize(id.getPath());
    }

    public static String resolve(ResourceKey<? extends Registry<?>> registry, Identifier id) {
        String path = registry.identifier().getPath();
        String domain = switch (path) {
            case "enchantment_effect_component_type", "enchantment_entity_effect_type", "enchantment_level_based_value_type", "enchantment_location_based_effect_type" -> "effect";
            default -> path;
        };
        return resolve(domain, id);
    }

    private static String humanize(String raw) {
        if (raw == null || raw.isBlank())
            return "";
        String clean = raw.contains(":") ? raw.substring(raw.indexOf(':') + 1) : raw;
        String leaf = clean.contains("/") ? clean.substring(clean.lastIndexOf('/') + 1) : clean;
        String[] parts = leaf.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank())
                continue;
            if (!builder.isEmpty())
                builder.append(' ');
            builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            builder.append(part.substring(1));
        }
        return builder.toString();
    }

    private StudioText() {}
}

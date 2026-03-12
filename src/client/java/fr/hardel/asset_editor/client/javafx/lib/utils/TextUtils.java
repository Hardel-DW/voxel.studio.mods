package fr.hardel.asset_editor.client.javafx.lib.utils;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.Locale;

public final class TextUtils {

    public static String resolveDisplayName(Identifier id, ResourceKey<? extends Registry<?>> registry) {
        String key = registry.identifier().getPath() + "." + id.getNamespace() + "." + id.getPath();
        return I18n.exists(key) ? I18n.get(key) : humanize(id.getPath());
    }

    public static String humanize(String raw) {
        String clean = raw.contains(":") ? raw.substring(raw.indexOf(':') + 1) : raw;
        String[] parts = clean.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            builder.append(part.substring(1));
        }
        return builder.toString();
    }

    private TextUtils() {}
}

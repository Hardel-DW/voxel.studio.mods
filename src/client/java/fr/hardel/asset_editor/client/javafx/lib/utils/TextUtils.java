package fr.hardel.asset_editor.client.javafx.lib.utils;

import java.util.Locale;

public final class TextUtils {

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

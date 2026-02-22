package fr.hardel.asset_editor.client.javafx.lib.utils;

import java.util.Locale;

public final class TextUtils {

    public static String toDisplay(String input) {
        if (input == null || input.isBlank()) return "";
        String clean = input.startsWith("#") ? input.substring(1) : input;
        int namespaceSep = clean.indexOf(':');
        String resource = namespaceSep >= 0 ? clean.substring(namespaceSep + 1) : clean;
        String[] path = resource.split("/");
        String leaf = path[path.length - 1];
        String[] words = leaf.replace('_', ' ').trim().split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (builder.length() > 0) builder.append(' ');
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) builder.append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return builder.toString();
    }

    private TextUtils() {
    }
}

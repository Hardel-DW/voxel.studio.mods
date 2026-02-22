package fr.hardel.asset_editor.client.javafx.components.ui.tree;

import java.util.Locale;

public final class TreeTextUtil {

    public static String toDisplay(String input) {
        if (input == null || input.isBlank()) return "";
        String clean = input.startsWith("#") ? input.substring(1) : input;
        int namespaceSep = clean.indexOf(':');
        String resource = namespaceSep >= 0 ? clean.substring(namespaceSep + 1) : clean;
        String[] pathSegments = resource.split("/");
        String leaf = pathSegments.length == 0 ? resource : pathSegments[pathSegments.length - 1];
        return capitalizeWords(leaf.replace('_', ' '));
    }

    private static String capitalizeWords(String value) {
        String[] words = value.trim().split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.isEmpty()) continue;
            if (i > 0) builder.append(' ');
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) builder.append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return builder.toString();
    }

    private TreeTextUtil() {
    }
}

package fr.hardel.asset_editor.client.javafx.components.ui.codeblock;

import fr.hardel.asset_editor.client.javafx.lib.highlight.Highlight;
import fr.hardel.asset_editor.client.javafx.lib.highlight.HighlightPalette;
import fr.hardel.asset_editor.client.javafx.lib.highlight.HighlightRegistry;
import fr.hardel.asset_editor.client.javafx.lib.highlight.HighlightStyle;
import fr.hardel.asset_editor.client.javafx.lib.utils.JsonTokenizer;
import javafx.scene.paint.Color;

import java.util.EnumMap;
import java.util.Map;

public final class JsonCodeBlockHighlighter implements CodeBlockHighlighter {

    public static final String STRING = "json-string";
    public static final String NUMBER = "json-number";
    public static final String BOOLEAN = "json-boolean";
    public static final String NULL = "json-null";
    public static final String PROPERTY = "json-property";
    public static final String PUNCTUATION = "json-punctuation";

    @Override
    public void apply(String text, HighlightRegistry registry) {
        Map<JsonTokenizer.TokenType, Highlight> highlightsByType = new EnumMap<>(JsonTokenizer.TokenType.class);

        for (JsonTokenizer.Token token : JsonTokenizer.tokenize(text)) {
            if (token.type() == JsonTokenizer.TokenType.WHITESPACE)
                continue;

            Highlight highlight = highlightsByType.computeIfAbsent(token.type(), ignored -> new Highlight());
            highlight.add(token.start(), token.end());
        }

        for (Map.Entry<JsonTokenizer.TokenType, Highlight> entry : highlightsByType.entrySet()) {
            String name = entry.getKey().highlightName();
            if (name != null)
                registry.set(name, entry.getValue());
        }
    }

    public static void installDefaultPalette(HighlightPalette palette) {
        palette.set(STRING, HighlightStyle.foreground(Color.web("#98c379")));
        palette.set(NUMBER, HighlightStyle.foreground(Color.web("#d19a66")));
        palette.set(BOOLEAN, HighlightStyle.foreground(Color.web("#56b6c2")));
        palette.set(NULL, HighlightStyle.foreground(Color.web("#c678dd")));
        palette.set(PROPERTY, HighlightStyle.foreground(Color.web("#61afef")));
        palette.set(PUNCTUATION, HighlightStyle.foreground(Color.web("#abb2bf")));
    }
}
